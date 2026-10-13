package com.wastina.trust.service;

import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.dto.EmploymentCreateRequestDto;
import com.wastina.trust.entity.EmploymentRecord;
import com.wastina.trust.entity.EmploymentType;
import com.wastina.trust.entity.EmploymentVerificationRequest;
import com.wastina.trust.entity.EmploymentVerificationStatus;
import com.wastina.trust.entity.SectorType;
import com.wastina.trust.entity.User;
import com.wastina.trust.entity.WorkRelationshipType;
import com.wastina.trust.repository.EmploymentRecordRepository;
import com.wastina.trust.repository.EmploymentVerificationRequestRepository;
import com.wastina.trust.repository.OrganizationMembershipRepository;
import com.wastina.trust.repository.OrganizationRepository;
import com.wastina.trust.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employment records and employer verification.
 *
 * <p>The two legacy overloads that accepted a bare {@code jobTitleEn} and defaulted every other
 * field have been removed. They bypassed validation and let a caller create a record without any of
 * the required fields. The DTO-driven entry point is now the only way in, and the controller has
 * already confirmed the caller belongs to the target organization.
 */
@Service
public class EmploymentService {

    /** States in which a verification request is still outstanding. */
    private static final java.util.List<com.wastina.trust.entity.EmploymentVerificationRequestStatus>
            OPEN_VERIFICATION_STATES = java.util.List.of(
            com.wastina.trust.entity.EmploymentVerificationRequestStatus.CREATED,
            com.wastina.trust.entity.EmploymentVerificationRequestStatus.SENT,
            com.wastina.trust.entity.EmploymentVerificationRequestStatus.DELIVERED,
            com.wastina.trust.entity.EmploymentVerificationRequestStatus.VIEWED,
            com.wastina.trust.entity.EmploymentVerificationRequestStatus.IN_REVIEW);

    private final EmploymentRecordRepository employmentRecordRepository;
    private final EmploymentVerificationRequestRepository employmentVerificationRequestRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmploymentValidationService employmentValidationService;

    public EmploymentService(EmploymentRecordRepository employmentRecordRepository,
                            EmploymentVerificationRequestRepository employmentVerificationRequestRepository,
                            OrganizationMembershipRepository organizationMembershipRepository,
                            OrganizationRepository organizationRepository,
                            UserRepository userRepository,
                            EmploymentValidationService employmentValidationService) {
        this.employmentRecordRepository = employmentRecordRepository;
        this.employmentVerificationRequestRepository = employmentVerificationRequestRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.employmentValidationService = employmentValidationService;
    }

    @Transactional
    public EmploymentRecord createEmploymentRecord(UUID workerId, EmploymentCreateRequestDto request) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker was not found: " + workerId));

        var organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization was not found: " + request.organizationId()));

        EmploymentRecord record = new EmploymentRecord(worker, organization, request.jobTitleEn());
        record.setJobTitleEn(request.jobTitleEn());
        record.setJobTitleAm(request.jobTitleAm());
        record.setOccupationCode(request.occupationCode());
        record.setDepartment(request.department());
        record.setLocation(request.location());
        record.setEmploymentType(request.employmentType());
        record.setWorkRelationshipType(request.workRelationshipType());
        record.setSector(request.sector());
        record.setStartDate(request.startDate());
        record.setEndDate(request.endDate());
        record.setCurrentEmployment(request.currentEmployment());
        record.setDescription(request.description());
        record.setVerificationStatus(EmploymentVerificationStatus.SELF_REPORTED);

        // Validate before persisting so a cross-field rule violation is a 400 with a useful message
        // rather than an exception raised from a @PrePersist callback during flush, which Hibernate
        // wraps in an InvalidDataAccessApiUsageException and would surface as a 500.
        employmentValidationService.validateEmploymentRecord(record);
        employmentValidationService.validateEmploymentDates(record);

        return employmentRecordRepository.save(record);
    }

    @Transactional
    public EmploymentVerificationRequest requestVerification(UUID employmentRecordId, UUID requesterId,
                                                              UUID organizationId) {
        EmploymentRecord record = employmentRecordRepository.findById(employmentRecordId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employment record was not found: " + employmentRecordId));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("Requester was not found: " + requesterId));

        if (!record.getWorker().getId().equals(requester.getId())) {
            throw new AccessDeniedException("Only the worker on the record can request verification.");
        }

        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization was not found: " + organizationId));

        if (!record.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException(
                    "Verification must target the organization on the employment record.");
        }

        // Refuse a duplicate open request instead of creating an unbounded stream of them.
        boolean alreadyOpen = employmentVerificationRequestRepository
                .existsByEmploymentRecord_IdAndStatusIn(employmentRecordId, OPEN_VERIFICATION_STATES);
        if (alreadyOpen) {
            throw new com.wastina.trust.controller.BusinessRuleViolationException(
                    "A verification request for this employment record is already open.");
        }

        EmploymentVerificationRequest request = new EmploymentVerificationRequest();
        request.setEmploymentRecord(record);
        request.setRequesterUser(requester);
        request.setTargetOrganization(organization);
        request.setVerificationMethod(com.wastina.trust.entity.VerificationMethod.EMPLOYER_PORTAL);
        request.create();

        employmentValidationService.validateVerificationRequest(request);

        return employmentVerificationRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<EmploymentRecord> listEmploymentForWorker(UUID workerId) {
        return employmentRecordRepository.findAllByWorker_IdOrderByStartDateDesc(workerId);
    }

    @Transactional(readOnly = true)
    public EmploymentRecord getEmploymentForWorker(UUID workerId, UUID employmentRecordId) {
        return employmentRecordRepository.findByIdAndWorker_Id(employmentRecordId, workerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employment record was not found: " + employmentRecordId));
    }

    @Transactional(readOnly = true)
    public List<EmploymentVerificationRequest> listVerificationRequestsForWorker(UUID workerId) {
        return employmentVerificationRequestRepository
                .findAllByRequesterUser_IdOrderByRequestedAtDesc(workerId);
    }

    /**
     * Lists the requests addressed to an organization. Requires an {@code ACTIVE} membership with
     * employment-verification authority; anything else is {@code 403}.
     */
    @Transactional(readOnly = true)
    public List<EmploymentVerificationRequest> listVerificationRequestsForOrganization(UUID userId,
                                                                                       UUID organizationId) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization was not found: " + organizationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User was not found: " + userId));

        var membership = organizationMembershipRepository.findByOrganizationAndUser(organization, user)
                .orElseThrow(() -> new AccessDeniedException("Organization membership is required."));
        employmentValidationService.validateVerifierAuthorization(membership);

        return employmentVerificationRequestRepository
                .findAllByTargetOrganization_IdOrderByRequestedAtDesc(organizationId);
    }

    @Transactional
    public EmploymentVerificationRequest beginVerificationReview(UUID verifierUserId, UUID organizationId,
                                                                 UUID requestId) {
        User verifier = userRepository.findById(verifierUserId)
                .orElseThrow(() -> new EntityNotFoundException("Verifier was not found: " + verifierUserId));
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization was not found: " + organizationId));

        var membership = organizationMembershipRepository.findByOrganizationAndUser(organization, verifier)
                .orElseThrow(() -> new AccessDeniedException("Organization membership is required."));
        employmentValidationService.validateVerifierAuthorization(membership);

        EmploymentVerificationRequest request = employmentVerificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Verification request was not found: " + requestId));

        if (!request.getTargetOrganization().getId().equals(organizationId)) {
            throw new AccessDeniedException("The request is not addressed to this organization.");
        }

        // Re-reviewing an already-decided request is a genuine state conflict and is reported as
        // 409 by the state machine rather than silently re-opened.
        request.markInReview();
        return employmentVerificationRequestRepository.save(request);
    }

    @Transactional
    public EmploymentVerificationRequest verifyEmployment(UUID verifierUserId, UUID organizationId,
                                                           UUID requestId) {
        User verifier = userRepository.findById(verifierUserId)
                .orElseThrow(() -> new EntityNotFoundException("Verifier was not found: " + verifierUserId));
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization was not found: " + organizationId));

        var membership = organizationMembershipRepository.findByOrganizationAndUser(organization, verifier)
                .orElseThrow(() -> new AccessDeniedException(
                        "Verifier is not a member of the organization"));
        employmentValidationService.validateVerifierAuthorization(membership);

        EmploymentVerificationRequest request = employmentVerificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Verification request was not found: " + requestId));

        if (!request.getTargetOrganization().getId().equals(organizationId)) {
            throw new AccessDeniedException("The request is not addressed to this organization.");
        }

        request.markVerified();
        request.getEmploymentRecord().setVerificationStatus(EmploymentVerificationStatus.VERIFIED);
        return employmentVerificationRequestRepository.save(request);
    }
}