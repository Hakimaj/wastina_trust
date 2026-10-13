package com.wastina.trust.service;

import com.wastina.trust.controller.ApiErrorCodes;
import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.entity.EmploymentRecord;
import com.wastina.trust.entity.EmploymentVerificationRequest;
import com.wastina.trust.entity.EmploymentVerificationRequestStatus;
import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Domain validation for employment records and employment-verification requests.
 *
 * <p>Change from the audited original: a caller who lacks employment-verification authority was
 * previously reported as {@code 409 Conflict} (via {@code IllegalStateException}), which is
 * indistinguishable from a genuine state conflict. It is now reported as
 * {@code 403 Forbidden}, so a client can tell "you may not do this" apart from "this is not
 * currently possible".
 */
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

    /**
     * The cross-field rule that {@code EmploymentRecord.validateState()} also enforces from its
     * {@code @PrePersist} callback.
     *
     * <p>Duplicated here on purpose: raising it during flush wraps the {@link IllegalArgumentException}
     * in a Hibernate exception, which the API reports as {@code 500}. Checking before the save
     * produces the intended {@code 400} with the original message.
     */
    public void validateEmploymentDates(EmploymentRecord record) {
        if (record.isCurrentEmployment() && record.getEndDate() != null) {
            throw new IllegalArgumentException(
                    "Current employment cannot have a closed end date. "
                            + "Use endDate = null for active employment.");
        }
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

    /**
     * Requires an {@code ACTIVE} membership whose role carries employment-verification authority.
     * Reported as {@code 403} rather than {@code 409}.
     */
    public void validateVerifierAuthorization(OrganizationMembership membership) {
        if (membership == null || membership.getStatus() == null) {
            throw new AccessDeniedException("Active organization membership is required to verify employment.");
        }
        if (membership.getStatus() != OrganizationMembershipStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "An active organization membership is required to verify employment. Current status: "
                            + membership.getStatus() + ".");
        }
        if (!membership.canVerifyEmployment()) {
            throw new AccessDeniedException(
                    "Membership role " + membership.getRole()
                            + " does not include employment-verification authority for this organization.");
        }
    }

    public void validateRequestLifecycle(EmploymentVerificationRequestStatus currentStatus,
                                         EmploymentVerificationRequestStatus targetStatus) {
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current verification request status is required.");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target verification request status is required.");
        }
    }
}