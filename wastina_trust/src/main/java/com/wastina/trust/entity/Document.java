package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_public_reference", columnNames = "public_reference"),
        @UniqueConstraint(name = "uk_document_object_version", columnNames = {"bucket", "object_key", "object_version"})
}, indexes = {
        @Index(name = "idx_document_owner", columnList = "owner_user_id"),
        @Index(name = "idx_document_status", columnList = "status")
})
@Data
@NoArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_reference", nullable = false, unique = true, length = 40)
    private String publicReference = "WST-DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 35)
    private DocumentType type;

    @Column(nullable = false, length = 50)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentClassification classification = DocumentClassification.RESTRICTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status = DocumentStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 20)
    private DocumentScanStatus scanStatus = DocumentScanStatus.NOT_SCANNED;

    @Column(name = "storage_provider", nullable = false, length = 40)
    private String storageProvider;

    @Column(nullable = false, length = 120)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 240)
    private String objectKey;

    @Column(name = "object_version", nullable = false, length = 160)
    private String objectVersion = "v1";

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "hash_algorithm", length = 20)
    private String hashAlgorithm;

    @Column(name = "content_size")
    private Long contentSize;

    @Column(name = "detected_mime_type", length = 100)
    private String detectedMimeType;

    @Column(name = "declared_mime_type", length = 100)
    private String declaredMimeType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "retention_until")
    private Instant retentionUntil;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    public void markQuarantined() {
        if (status != DocumentStatus.UPLOADING && status != DocumentStatus.UPLOAD_PENDING) {
            throw new IllegalStateException("Document is not awaiting quarantine.");
        }
        status = DocumentStatus.QUARANTINED;
        scanStatus = DocumentScanStatus.PENDING;
    }

    public void markAvailable(String detectedMimeType, long contentSize, String contentHash) {
        if (status != DocumentStatus.QUARANTINED || scanStatus != DocumentScanStatus.PASSED) {
            throw new IllegalStateException("Only clean quarantined documents can become available.");
        }
        this.detectedMimeType = detectedMimeType;
        this.contentSize = contentSize;
        this.contentHash = contentHash;
        this.hashAlgorithm = "SHA-256";
        this.status = DocumentStatus.AVAILABLE;
        this.uploadedAt = Instant.now();
    }

    public void reject() {
        if (status == DocumentStatus.AVAILABLE || status == DocumentStatus.DELETED) {
            throw new IllegalStateException("Available or deleted documents cannot be rejected in place.");
        }
        status = DocumentStatus.REJECTED;
        scanStatus = DocumentScanStatus.FAILED;
    }

    public boolean canIssueDownloadUrl() {
        return status == DocumentStatus.AVAILABLE || status == DocumentStatus.VERIFIED;
    }
}
