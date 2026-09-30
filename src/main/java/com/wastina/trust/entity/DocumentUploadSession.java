package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_upload_sessions", indexes = @Index(name = "idx_upload_session_document", columnList = "document_id"))
@Data
@NoArgsConstructor
public class DocumentUploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "allowed_size", nullable = false)
    private long allowedSize;

    @Column(name = "allowed_mime_types", nullable = false, length = 300)
    private String allowedMimeTypes = "application/pdf,image/jpeg,image/png";

    @Column(name = "upload_object_key", nullable = false, unique = true, length = 240)
    private String uploadObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadSessionStatus status = UploadSessionStatus.CREATED;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public void beginUpload() {
        if (status != UploadSessionStatus.CREATED || Instant.now().isAfter(expiresAt)) {
            throw new IllegalStateException("Upload session is not available.");
        }
        status = UploadSessionStatus.UPLOADING;
    }

    public void complete() {
        if (status != UploadSessionStatus.UPLOADING) {
            throw new IllegalStateException("Only an active upload can be completed.");
        }
        status = UploadSessionStatus.COMPLETED;
    }
}
