package com.wastina.trust.repository;

import com.wastina.trust.entity.EmploymentVerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmploymentVerificationRequestRepository extends JpaRepository<EmploymentVerificationRequest, UUID> {
}
