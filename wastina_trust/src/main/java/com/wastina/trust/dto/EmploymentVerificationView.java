package com.wastina.trust.dto;

import com.wastina.trust.entity.EmploymentVerificationRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmploymentVerificationView(
        UUID id,
        UUID employmentRecordId,
        String jobTitleEn,
        UUID organizationId,
        String organizationName,
        String status,
        String verificationMethod,
        LocalDateTime requestedAt,
        LocalDateTime responseAt) {
    public static EmploymentVerificationView from(EmploymentVerificationRequest request) {
        return new EmploymentVerificationView(
                request.getId(),
                request.getEmploymentRecord().getId(),
                request.getEmploymentRecord().getJobTitleEn(),
                request.getTargetOrganization().getId(),
                request.getTargetOrganization().getName(),
                request.getStatus().name(),
                request.getVerificationMethod().name(),
                request.getRequestedAt(),
                request.getResponseAt());
    }
}