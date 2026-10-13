package com.wastina.trust.service;

import com.wastina.trust.controller.ApiErrorCodes;
import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.entity.Document;
import com.wastina.trust.entity.DocumentClassification;
import com.wastina.trust.entity.DocumentScanStatus;
import com.wastina.trust.entity.DocumentStatus;
import com.wastina.trust.entity.DocumentType;
import com.wastina.trust.entity.DocumentUploadSession;
import com.wastina.trust.entity.UploadSessionStatus;
import com.wastina.trust.gateway.document.MalwareScanner;
import com.wastina.trust.gateway.document.ObjectStorageProvider;
import com.wastina.trust.repository.DocumentRepository;
import com.wastina.trust.repository.DocumentUploadSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document storage, quarantine, scanning and short-lived download access.
 *
 * <p>Changes from the audited original:
 * <ul>
 *   <li>{@code issueDownloadUrl} no longer accepts {@code requesterId} and {@code authorized}
 *       query parameters. The caller is derived from the JWT and access is decided from real
 *       ownership plus document state. Previously any authenticated user could download any
 *       document by appending {@code ?authorized=true}.</li>
 *   <li>Session ownership and expiry are enforced, so one caller cannot complete another's upload
 *       session.</li>
 *   <li>Detected MIME type is validated against the session's allow-list, so a caller cannot
 *       mislabel a payload as an accepted type and bypass the later scan.</li>
 *   <li>Listing is scoped to the caller and paged.</li>
 * </ul>
 *
 * <p>The quarantine-then-scan-then-publish order is preserved: content is written to a quarantine
 * bucket, scanned, and only a clean document is marked available.
 */
@Service
public class DocumentGatewayService {

    /** 20 MiB. Matches the {@code allowedSize} reported to the client in the session view. */
    public static final long DEFAULT_MAX_SIZE = 20L * 1024L * 1024L;

    private final DocumentRepository documentRepository;
    private final DocumentUploadSessionRepository uploadSessionRepository;
    private final ObjectStorageProvider storageProvider;
    private final MalwareScanner malwareScanner;

    public DocumentGatewayService(DocumentRepository documentRepository,
                                  DocumentUploadSessionRepository uploadSessionRepository,
                                  ObjectStorageProvider storageProvider,
                                  MalwareScanner malwareScanner) {
        this.documentRepository = documentRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.storageProvider = storageProvider;
        this.malwareScanner = malwareScanner;
    }

    /**
     * Opens an upload session for the caller. The owner is always the authenticated user, never a
     * value supplied in the body.
     */
    @Transactional
    public DocumentUploadSession createUploadSession(UUID ownerUserId, DocumentType type, String purpose,
                                                     String declaredMimeType) {
        if (ownerUserId == null || type == null || purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Owner, document type, and purpose are required.");
        }

        Document document = new Document();
        document.setOwnerUserId(ownerUserId);
        document.setType(type);
        document.setPurpose(purpose.trim());
        document.setDeclaredMimeType(declaredMimeType);
        document.setClassification(DocumentClassification.RESTRICTED);
        document.setStorageProvider(storageProvider.providerName());
        document.setBucket("wastina-quarantine");
        document.setObjectKey("objects/quarantine/" + java.time.Year.now() + "/" + UUID.randomUUID());
        document.setStatus(DocumentStatus.UPLOAD_PENDING);
        Document saved = documentRepository.save(document);

        DocumentUploadSession session = new DocumentUploadSession();
        session.setDocument(saved);
        session.setOwnerUserId(ownerUserId);
        session.setAllowedSize(DEFAULT_MAX_SIZE);
        session.setUploadObjectKey(saved.getObjectKey());
        session.setExpiresAt(Instant.now().plusSeconds(900));
        return uploadSessionRepository.save(session);
    }

    /**
     * Completes an upload: writes to quarantine, scans, and publishes only if clean.
     *
     * @param requesterUserId the authenticated caller; must own the session
     */
    @Transactional
    public Document completeUpload(UUID sessionId, UUID requesterUserId, byte[] content,
                                   String detectedMimeType) {
        DocumentUploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Upload session was not found: " + sessionId));
        

        // Ownership: a caller may only complete their own session.
        if (!session.getOwnerUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("This upload session belongs to another account.");
        }
        if (session.getStatus() != UploadSessionStatus.CREATED) {
            throw new BusinessRuleViolationException(
                    "Upload session is not open. Current status: " + session.getStatus() + ".");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleViolationException("Upload session has expired.");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("The uploaded file is empty.");
        }
        if (content.length > session.getAllowedSize()) {
            throw new IllegalArgumentException("The uploaded file exceeds the permitted size of "
                    + session.getAllowedSize() + " bytes.");
        }

        String normalizedMime = detectedMimeType == null
                ? "" : detectedMimeType.trim().toLowerCase(Locale.ROOT);
        if (!isPermittedMimeType(session, normalizedMime)) {
            throw new IllegalArgumentException(
                    "Content type '" + normalizedMime + "' is not accepted for this upload session. "
                            + "Permitted: " + session.getAllowedMimeTypes() + ".");
        }

        session.beginUpload();
        Document document = session.getDocument();

        storageProvider.putQuarantined(document.getBucket(), document.getObjectKey(), content);
        document.markQuarantined();

        MalwareScanner.ScanResult result = malwareScanner.scan(content, normalizedMime);
        if (!result.clean()) {
            document.reject();
            session.complete();
            uploadSessionRepository.save(session);
            return documentRepository.save(document);
        }

        document.setScanStatus(DocumentScanStatus.PASSED);
        document.markAvailable(normalizedMime, content.length, sha256(content));
        session.complete();
        uploadSessionRepository.save(session);
        return documentRepository.save(document);
    }

    /**
     * Issues a short-lived download URL.
     *
     * <p>Authorization is derived entirely server-side from document ownership and state. The caller
     * must be the owner, and the document must be in a downloadable state.
     */
    @Transactional(readOnly = true)
    public String issueDownloadUrl(UUID documentId, UUID requesterUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document was not found: " + documentId));

        if (!document.getOwnerUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("Document access is forbidden.");
        }
        if (!document.canIssueDownloadUrl()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "Document is not available for download. Current status: " + document.getStatus() + ".");
        }
        return storageProvider.createDownloadUrl(
                document.getBucket(), document.getObjectKey(), document.getObjectVersion(), 300);
    }

    @Transactional(readOnly = true)
    public Page<Document> listForOwner(UUID ownerUserId, Pageable pageable) {
        return documentRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Document getForOwner(UUID documentId, UUID requesterUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document was not found: " + documentId));
        if (!document.getOwnerUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("Document access is forbidden.");
        }
        return document;
    }

    /**
     * Marks a document for deletion. Content is not destroyed here: retention and legal-hold rules
     * are out of scope, so this records intent and leaves the object for a retention process.
     */
    @Transactional
    public Document requestDeletion(UUID documentId, UUID requesterUserId) {
        Document document = getForOwner(documentId, requesterUserId);
        if (document.getStatus() == DocumentStatus.DELETED) {
            throw new BusinessRuleViolationException("Document is already deleted.");
        }
        if (document.getStatus() == DocumentStatus.RETENTION_HOLD) {
            throw new BusinessRuleViolationException("Document is under retention hold and cannot be deleted.");
        }
        document.setStatus(DocumentStatus.DELETION_PENDING);
        document.setDeletedAt(Instant.now());
        return documentRepository.save(document);
    }

    private boolean isPermittedMimeType(DocumentUploadSession session, String normalizedMime) {
        if (normalizedMime.isEmpty()) {
            return false;
        }
        List<String> permitted = Arrays.stream(session.getAllowedMimeTypes().split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .toList();
        return permitted.contains(normalizedMime);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate document hash", exception);
        }
    }
}