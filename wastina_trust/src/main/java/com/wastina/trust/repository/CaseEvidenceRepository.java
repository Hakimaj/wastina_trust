package com.wastina.trust.repository;

import com.wastina.trust.entity.CaseEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseEvidenceRepository extends JpaRepository<CaseEvidence, UUID> {
}
