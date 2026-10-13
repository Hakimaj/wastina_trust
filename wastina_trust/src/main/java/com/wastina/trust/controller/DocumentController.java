package com.wastina.trust.controller;

import com.wastina.trust.dto.DocumentUploadSessionView;
import com.wastina.trust.dto.DocumentView;
import com.wastina.trust.entity.DocumentType;
import com.wastina.trust.entity.User;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.DocumentGatewayService;
import com.wastina.trust.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document upload, listing and download.
 *
 * <p>Changes from the audited original:
 * <ul>
 *   <li>Upload is {@code multipart/form-data}. The previous endpoint took a raw {@code byte[]}
 *       body, which a browser {@code <input type="file">} could not produce without hand-building
 *       an array-of-numbers payload. {@code detectedMimeType} is now a form field rather than a
 *       mandatory header.</li>
 *   <li>Download no longer accepts {@code requesterId} or {@code authorized}. Both were
 *       client-asserted and trivially spoofable, which let any authenticated user download any
 *       document.</li>
 *   <li>The owner is always the authenticated caller.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Document upload and download")
public class DocumentController {

    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_MIME_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private final DocumentGatewayService documentGatewayService;
    private final CurrentUserResolver currentUserResolver;

    public DocumentController(DocumentGatewayService documentGatewayService,
                              CurrentUserResolver currentUserResolver) {
        this.documentGatewayService = documentGatewayService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/upload-sessions")
    @Operation(summary = "Open an upload session for the caller")
    public ResponseEntity<DocumentUploadSessionView> createUploadSession(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUploadSessionRequest request) {
        User caller = currentUserResolver.requireUser(jwt);

        var session = documentGatewayService.createUploadSession(
                caller.getId(), request.type(), request.purpose(), request.declaredMimeType());

        return ResponseEntity
                .created(URI.create("/api/v1/documents/upload-sessions/" + session.getId()))
                .body(DocumentUploadSessionView.from(session));
    }

    /**
     * Completes an upload using {@code multipart/form-data}.
     *
     * <p>The browser contract is: {@code file} part plus an optional {@code detectedMimeType} field.
     * When the field is omitted the declared MIME type from the session is used, and the value is
     * still validated against the session's allow-list before anything is stored.
     */
    @PostMapping(path = "/upload-sessions/{sessionId}/complete",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the file for a session (multipart/form-data)")
    public ResponseEntity<DocumentView> completeUpload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "detectedMimeType", required = false) String detectedMimeType)
            throws IOException {

        User caller = currentUserResolver.requireUser(jwt);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty.");
        }

        String mimeType = detectedMimeType == null || detectedMimeType.isBlank()
                ? resolveMimeType(file)
                : detectedMimeType;

        var document = documentGatewayService.completeUpload(
                sessionId, caller.getId(), file.getBytes(), mimeType);

        return ResponseEntity.ok(DocumentView.from(document));
    }

    @GetMapping
    @Operation(summary = "List the caller's documents (paged)")
    public ResponseEntity<PageResponse<DocumentView>> listDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User caller = currentUserResolver.requireUser(jwt);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        var result = documentGatewayService.listForOwner(
                caller.getId(),
                PageRequest.of(Math.max(page, 0), pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(PageResponse.of(result,
                result.getContent().stream().map(DocumentView::from).toList()));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Read one of the caller's documents")
    public DocumentView getDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        User caller = currentUserResolver.requireUser(jwt);
        return DocumentView.from(documentGatewayService.getForOwner(documentId, caller.getId()));
    }

    /**
     * Returns a short-lived download URL. Access is decided server-side from ownership and document
     * state; there is no client-supplied authorization flag.
     */
    @GetMapping("/{documentId}/download-url")
    @Operation(summary = "Issue a short-lived download URL for one of the caller's documents")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        User caller = currentUserResolver.requireUser(jwt);
        String url = documentGatewayService.issueDownloadUrl(documentId, caller.getId());
        return ResponseEntity.ok(new DownloadUrlResponse(url, 300));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Mark one of the caller's documents for deletion")
    public DocumentView deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId) {
        User caller = currentUserResolver.requireUser(jwt);
        return DocumentView.from(documentGatewayService.requestDeletion(documentId, caller.getId()));
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? DEFAULT_MIME_TYPE : contentType;
    }

    public record CreateUploadSessionRequest(
            @NotNull(message = "is required") DocumentType type,

            @NotBlank(message = "is required")
            @Size(max = 50, message = "must be at most 50 characters")
            String purpose,

            @Size(max = 100, message = "must be at most 100 characters")
            String declaredMimeType) {
    }

    /**
     * @param url               the pre-signed URL
     * @param expiresInSeconds remaining lifetime of the URL
     */
    public record DownloadUrlResponse(String url, int expiresInSeconds) {
    }
}