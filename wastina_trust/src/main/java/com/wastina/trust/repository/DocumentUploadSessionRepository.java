package com.wastina.trust.repository;

import com.wastina.trust.entity.DocumentUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentUploadSessionRepository extends JpaRepository<DocumentUploadSession, UUID> {
}
