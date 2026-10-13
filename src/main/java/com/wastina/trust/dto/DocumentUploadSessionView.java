package com.wastina.trust.dto;

import com.wastina.trust.entity.DocumentUploadSession;
import com.wastina.trust.entity.UploadSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * An open upload session.
 *
 * <p>The response tells the client exactly what the upload endpoint expects: the maximum size and
 * the permitted MIME types, so a browser {@code <input type="file">} can be validated before
 * anything is transmitted.
 */
@Schema(name = "DocumentUploadSession")
public record DocumentUploadSessionView(
        @Schema(description = "Upload session identifier")
        UUID id,

        @Schema(description = "Document this session will create")
        UUID documentId,

        @Schema(description = "Public reference of the document")
        String documentPublicReference,

        @Schema(description = "Maximum permitted upload size in bytes")
        long allowedSize,

        @Schema(description = "Permitted detected MIME types")
        List<String> allowedMimeTypes,

        @Schema(description = "Session state")
        UploadSessionStatus status,

        @Schema(description = "When the session expires; uploads after this are refused")
        Instant expiresAt) {

    public static DocumentUploadSessionView from(DocumentUploadSession session) {
        return new DocumentUploadSessionView(
                session.getId(),
                session.getDocument().getId(),
                session.getDocument().getPublicReference(),
                session.getAllowedSize(),
                Arrays.stream(session.getAllowedMimeTypes().split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList(),
                session.getStatus(),
                session.getExpiresAt());
    }
}