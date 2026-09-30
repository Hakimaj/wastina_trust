package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import com.wastina.trust.gateway.document.MalwareScanner;
import com.wastina.trust.gateway.document.ObjectStorageProvider;
import com.wastina.trust.repository.DocumentRepository;
import com.wastina.trust.repository.DocumentUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentGatewayService {
    private static final long DEFAULT_MAX_SIZE = 20L * 1024L * 1024L;
    private final DocumentRepository documentRepository;
    private final DocumentUploadSessionRepository uploadSessionRepository;
    private final ObjectStorageProvider storageProvider;
    private final MalwareScanner malwareScanner;

    @Transactional
    public DocumentUploadSession createUploadSession(UUID ownerUserId, DocumentType type, String purpose,
                                                     String declaredMimeType) {
        if (ownerUserId == null || type == null || purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Owner, document type, and purpose are required.");
        }
        Document document = new Document();
        document.setOwnerUserId(ownerUserId);
        document.setType(type);
        document.setPurpose(purpose);
        document.setDeclaredMimeType(declaredMimeType);
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

    @Transactional
    public Document completeUpload(UUID sessionId, byte[] content, String detectedMimeType) {
        DocumentUploadSession session = uploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found"));
        if (content == null || content.length == 0 || content.length > session.getAllowedSize()) {
            throw new IllegalArgumentException("File is empty or exceeds the upload limit.");
        }
        session.beginUpload();
        Document document = session.getDocument();
        storageProvider.putQuarantined(document.getBucket(), document.getObjectKey(), content);
        document.markQuarantined();
        MalwareScanner.ScanResult result = malwareScanner.scan(content, detectedMimeType);
        if (!result.clean()) {
            document.reject();
            session.complete();
            uploadSessionRepository.save(session);
            return documentRepository.save(document);
        }
        document.setScanStatus(DocumentScanStatus.PASSED);
        document.markAvailable(detectedMimeType, content.length, sha256(content));
        session.complete();
        uploadSessionRepository.save(session);
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public String issueDownloadUrl(UUID documentId, UUID requesterId, boolean authorized) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (!authorized || !document.getOwnerUserId().equals(requesterId) || !document.canIssueDownloadUrl()) {
            throw new SecurityException("Document access is forbidden.");
        }
        return storageProvider.createDownloadUrl(document.getBucket(), document.getObjectKey(), document.getObjectVersion(), 300);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate document hash", exception);
        }
    }
}
