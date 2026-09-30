package com.wastina.trust.repository;

import com.wastina.trust.entity.ComplianceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, UUID> {
}
