package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import com.wastina.trust.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmploymentService {
    private final EmploymentRecordRepository employmentRecordRepository;
    private final EmploymentVerificationRequestRepository employmentVerificationRequestRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmploymentValidationService employmentValidationService;

    @Transactional
    public EmploymentRecord createEmploymentRecord(UUID workerId, UUID organizationId, String jobTitleEn) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        EmploymentRecord record = new EmploymentRecord(worker, organization, jobTitleEn);
        record.setJobTitleEn(jobTitleEn);
        record.setVerificationStatus(EmploymentVerificationStatus.SELF_REPORTED);
        employmentValidationService.validateEmploymentRecord(record);

        return employmentRecordRepository.save(record);
    }

    @Transactional
    public EmploymentVerificationRequest requestVerification(UUID employmentRecordId, UUID requesterId, UUID organizationId) {
        EmploymentRecord record = employmentRecordRepository.findById(employmentRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Employment record not found"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        EmploymentVerificationRequest request = new EmploymentVerificationRequest();
        request.setEmploymentRecord(record);
        request.setRequesterUser(requester);
        request.setTargetOrganization(organization);
        request.setVerificationMethod(VerificationMethod.EMPLOYER_PORTAL);
        request.create();

        employmentValidationService.validateVerificationRequest(request);

        return employmentVerificationRequestRepository.save(request);
    }

    @Transactional
    public EmploymentVerificationRequest verifyEmployment(UUID verifierUserId, UUID organizationId, UUID requestId) {
        User verifier = userRepository.findById(verifierUserId)
                .orElseThrow(() -> new IllegalArgumentException("Verifier not found"));
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        OrganizationMembership membership = organizationMembershipRepository.findByOrganizationAndUser(organization, verifier)
                .orElseThrow(() -> new IllegalArgumentException("Verifier is not a member of the organization"));

        employmentValidationService.validateVerifierAuthorization(membership);

        EmploymentVerificationRequest request = employmentVerificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found"));

        if (!request.getTargetOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Verifier is not authorized for the target organization.");
        }

        request.markVerified();
        request.getEmploymentRecord().setVerificationStatus(EmploymentVerificationStatus.VERIFIED);
        return employmentVerificationRequestRepository.save(request);
    }
}
