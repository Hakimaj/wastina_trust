package com.wastina.trust.repository;

import com.wastina.trust.entity.ComplianceAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplianceActionRepository extends JpaRepository<ComplianceAction, UUID> {
}
