package com.wastina.trust.service;

import com.wastina.trust.entity.EmploymentRecord;
import com.wastina.trust.entity.EmploymentVerificationRequest;
import com.wastina.trust.entity.EmploymentVerificationRequestStatus;
import com.wastina.trust.entity.OrganizationMembership;
import org.springframework.stereotype.Service;

@Service
public class EmploymentValidationService {

    public void validateEmploymentRecord(EmploymentRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Employment record is required.");
        }
        if (record.getWorker() == null) {
            throw new IllegalArgumentException("Worker must be set on employment record.");
        }
        if (record.getOrganization() == null) {
            throw new IllegalArgumentException("Organization must be set on employment record.");
        }
        EmploymentRecord.validateDates(record.getStartDate(), record.getEndDate());
    }

    public void validateVerificationRequest(EmploymentVerificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Verification request is required.");
        }
        if (request.getEmploymentRecord() == null) {
            throw new IllegalArgumentException("Employment record is required for a verification request.");
        }
        if (request.getTargetOrganization() == null) {
            throw new IllegalArgumentException("Target organization is required for a verification request.");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Verification request status is required.");
        }
    }

    public void validateVerifierAuthorization(OrganizationMembership membership) {
        if (membership == null || membership.getStatus() == null) {
            throw new IllegalArgumentException("Active organization membership is required to verify employment.");
        }
        if (!membership.canVerifyEmployment()) {
            throw new IllegalStateException("User does not have employer-verification authority for this organization.");
        }
    }

    public void validateRequestLifecycle(EmploymentVerificationRequestStatus currentStatus, EmploymentVerificationRequestStatus targetStatus) {
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current verification request status is required.");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target verification request status is required.");
        }
    }
}
