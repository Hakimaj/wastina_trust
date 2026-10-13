package com.wastina.trust.dto;

import com.wastina.trust.entity.Document;
import com.wastina.trust.entity.DocumentScanStatus;
import com.wastina.trust.entity.DocumentStatus;
import com.wastina.trust.entity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * A document metadata record.
 *
 * <p>Storage details ({@code bucket}, {@code objectKey}, {@code objectVersion}) are deliberately
 * excluded: exposing them would let a client reason about or address the object store directly.
 * Downloads are only ever obtained through the short-lived, authorized download URL.
 */
@Schema(name = "Document")
public record DocumentView(
        @Schema(description = "Stable public reference, e.g. WST-DOC-1A2B3C4D")
        String publicReference,

        @Schema(description = "Database identifier; treat as opaque")
        UUID id,

        @Schema(description = "Document category")
        DocumentType type,

        @Schema(description = "Why the document is held")
        String purpose,

        @Schema(description = "Lifecycle state")
        DocumentStatus status,

        @Schema(description = "Malware-scan state")
        DocumentScanStatus scanStatus,

        @Schema(description = "Content type detected from the bytes, not the filename")
        String detectedMimeType,

        @Schema(description = "Size in bytes")
        Long contentSize,

        @Schema(description = "SHA-256 of the stored content")
        String contentHash,

        @Schema(description = "When the document was created")
        Instant createdAt,

        @Schema(description = "When the upload completed")
        Instant uploadedAt,

        @Schema(description = "True when the caller may currently obtain a download URL")
        boolean downloadable) {

    public static DocumentView from(Document document) {
        return new DocumentView(
                document.getPublicReference(),
                document.getId(),
                document.getType(),
                document.getPurpose(),
                document.getStatus(),
                document.getScanStatus(),
                document.getDetectedMimeType(),
                document.getContentSize(),
                document.getContentHash(),
                document.getCreatedAt(),
                document.getUploadedAt(),
                document.canIssueDownloadUrl());
    }
}