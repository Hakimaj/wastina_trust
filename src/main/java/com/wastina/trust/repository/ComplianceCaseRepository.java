package com.wastina.trust.repository;

import com.wastina.trust.entity.ComplianceCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplianceCaseRepository extends JpaRepository<ComplianceCase, UUID> {
}
