package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import com.wastina.trust.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplianceService {
    private final ComplaintRepository complaintRepository;
    private final ComplianceCaseRepository caseRepository;
    private final CaseEvidenceRepository evidenceRepository;
    private final ComplianceEventRepository eventRepository;
    private final AppealRepository appealRepository;
    private final ComplianceActionRepository actionRepository;
    private final UserRepository userRepository;
    private final ComplianceAuthorizationService authorizationService;

    @Transactional
    public Complaint submitComplaint(UUID complainantId, UUID subjectId, ComplaintCategory category,
                                     String description, String relationshipType,
                                     String sourceEntityType, String sourceEntityId) {
        if (complainantId.equals(subjectId)) {
            throw new IllegalArgumentException("A complaint subject must be distinct from its complainant.");
        }
        User complainant = userRepository.findById(complainantId)
                .orElseThrow(() -> new IllegalArgumentException("Complainant not found"));
        User subject = userRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        Complaint complaint = new Complaint();
        complaint.setComplainant(complainant);
        complaint.setSubject(subject);
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setRelationshipType(relationshipType);
        complaint.setSourceEntityType(sourceEntityType);
        complaint.setSourceEntityId(sourceEntityId);
        complaint.submit();
        Complaint saved = complaintRepository.save(complaint);
        eventRepository.save(ComplianceEvent.outcome(ComplianceEventType.COMPLAINT_SUBMITTED, null, subjectId));
        return saved;
    }

    @Transactional
    public ComplianceCase openCase(UUID complaintId, UUID subjectId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        ComplianceCase complianceCase = new ComplianceCase();
        complianceCase.setComplaint(complaint);
        complianceCase.setSubject(userRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found")));
        complianceCase.open();
        complaint.setStatus(ComplaintStatus.CONVERTED_TO_CASE);
        ComplianceCase saved = caseRepository.save(complianceCase);
        eventRepository.save(ComplianceEvent.outcome(ComplianceEventType.CASE_OPENED, saved, subjectId));
        return saved;
    }

        @Transactional
        public ComplianceCase startInvestigation(UUID caseId) {
                ComplianceCase complianceCase = caseRepository.findById(caseId)
                                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
                complianceCase.startInvestigation();
                ComplianceCase saved = caseRepository.save(complianceCase);
                eventRepository.save(ComplianceEvent.outcome(ComplianceEventType.INVESTIGATION_STARTED, saved,
                                saved.getSubject().getId()));
                return saved;
        }

    @Transactional
    public ComplianceCase submitDecision(UUID caseId, ComplianceRole role, boolean caseScopeGranted,
                                         DecisionType decisionType, String reason) {
        authorizationService.requireDecisionAuthority(role, caseScopeGranted);
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        complianceCase.beginDecision();
        complianceCase.setDecisionReason(reason);
        complianceCase.decide(decisionType);
        ComplianceCase saved = caseRepository.save(complianceCase);
        ComplianceEventType eventType = switch (decisionType) {
            case UPHELD -> ComplianceEventType.CASE_UPHELD;
            case PARTIALLY_UPHELD -> ComplianceEventType.CASE_PARTIALLY_UPHELD;
            default -> ComplianceEventType.CASE_DISMISSED;
        };
        eventRepository.save(ComplianceEvent.outcome(eventType, saved, saved.getSubject().getId()));
        return saved;
    }

    @Transactional
    public Appeal submitAppeal(UUID caseId, UUID appellantId, AppealGrounds grounds) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        complianceCase.markAppealed();
        Appeal appeal = new Appeal(complianceCase, appellantId, grounds);
        appeal.submit();
        Appeal saved = appealRepository.save(appeal);
        eventRepository.save(ComplianceEvent.outcome(ComplianceEventType.APPEAL_SUBMITTED, complianceCase, complianceCase.getSubject().getId()));
        return saved;
    }

    @Transactional
    public ComplianceAction applyTemporaryAction(UUID caseId, UUID actorId, ComplianceActionType type,
                                                 String reason, java.time.Instant expiresAt) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        ComplianceAction action = new ComplianceAction();
        action.setComplianceCase(complianceCase);
        action.setAppliedBy(actorId);
        action.setType(type);
        action.setReason(reason);
        action.setExpiresAt(expiresAt);
        action.activate();
        ComplianceAction saved = actionRepository.save(action);
        eventRepository.save(ComplianceEvent.outcome(ComplianceEventType.TEMPORARY_RESTRICTION_APPLIED, complianceCase, complianceCase.getSubject().getId()));
        return saved;
    }
}
