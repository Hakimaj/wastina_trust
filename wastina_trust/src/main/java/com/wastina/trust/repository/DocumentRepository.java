package com.wastina.trust.repository;

import com.wastina.trust.entity.Document;
import com.wastina.trust.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByPublicReference(String publicReference);

    Page<Document> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    List<Document> findAllByOwnerUserIdAndStatus(UUID ownerUserId, DocumentStatus status);
}