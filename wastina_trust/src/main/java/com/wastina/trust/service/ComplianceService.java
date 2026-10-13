package com.wastina.trust.service;

import com.wastina.trust.controller.ApiErrorCodes;
import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.entity.Appeal;
import com.wastina.trust.entity.AppealGrounds;
import com.wastina.trust.entity.Complaint;
import com.wastina.trust.entity.ComplaintCategory;
import com.wastina.trust.entity.ComplianceAction;
import com.wastina.trust.entity.ComplianceActionType;
import com.wastina.trust.entity.ComplianceCase;
import com.wastina.trust.entity.ComplianceEvent;
import com.wastina.trust.entity.ComplianceEventType;
import com.wastina.trust.entity.ComplianceRole;
import com.wastina.trust.entity.DecisionType;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.AppealRepository;
import com.wastina.trust.repository.ComplaintRepository;
import com.wastina.trust.repository.ComplianceActionRepository;
import com.wastina.trust.repository.ComplianceCaseRepository;
import com.wastina.trust.repository.ComplianceEventRepository;
import com.wastina.trust.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Complaints and dispute resolution.
 *
 * <p>Actor identity is now always passed in from the controller, which derives it from the JWT. The
 * audited original had each controller pass a body-supplied actor id, which allowed any
 * authenticated user to file or escalate a complaint as another person.
 *
 * <p>{@link #resolveRoleFor} and {@link #hasCaseScope} exist so a client cannot assert its own
 * compliance authority: the role comes from persisted roles and the scope from real case
 * membership.
 */
@Service
public class ComplianceService {

    private final ComplaintRepository complaintRepository;
    private final ComplianceCaseRepository caseRepository;
    private final AppealRepository appealRepository;
    private final ComplianceActionRepository actionRepository;
    private final ComplianceEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ComplianceAuthorizationService authorizationService;

    public ComplianceService(ComplaintRepository complaintRepository,
                             ComplianceCaseRepository caseRepository,
                             AppealRepository appealRepository,
                             ComplianceActionRepository actionRepository,
                             ComplianceEventRepository eventRepository,
                             UserRepository userRepository,
                             ComplianceAuthorizationService authorizationService) {
        this.complaintRepository = complaintRepository;
        this.caseRepository = caseRepository;
        this.appealRepository = appealRepository;
        this.actionRepository = actionRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * Derives the {@link ComplianceRole} a user acts in, from their persisted application roles.
     * A caller cannot request a different role.
     */
    @Transactional(readOnly = true)
    public ComplianceRole resolveRoleFor(User user) {
        boolean administrator = user.getRoles().stream()
                .anyMatch(role -> role.getName() == com.wastina.trust.entity.ERole.ROLE_ADMIN);
        if (administrator) {
            return ComplianceRole.ADMIN;
        }
        boolean employer = user.getRoles().stream()
                .anyMatch(role -> role.getName() == com.wastina.trust.entity.ERole.ROLE_EMPLOYER);
        return employer ? ComplianceRole.CASE_REVIEWER : ComplianceRole.SUPPORT_AGENT;
    }

    /**
     * Computes case scope from real data rather than trusting a client-supplied boolean.
     *
     * <p>An administrator has scope on every case. Anyone else has scope only on a case concerning
     * an employment record they are party to, or a case they raised.
     */
    @Transactional(readOnly = true)
    public boolean hasCaseScope(User user, UUID caseId) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Compliance case was not found: " + caseId));

        if (complianceCase.getSubject() != null
                && complianceCase.getSubject().getId().equals(user.getId())) {
            return true;
        }
        Complaint complaint = complianceCase.getComplaint();
        if (complaint != null && complaint.getComplainant() != null
                && complaint.getComplainant().getId().equals(user.getId())) {
            return true;
        }
        return resolveRoleFor(user) == ComplianceRole.ADMIN;
    }

    @Transactional
    public Complaint submitComplaint(UUID complainantId, UUID subjectId, ComplaintCategory category,
                                     String description, String relationshipType,
                                     String sourceEntityType, String sourceEntityId) {
        if (complainantId.equals(subjectId)) {
            throw new IllegalArgumentException("A complaint subject must be distinct from its complainant.");
        }
        User complainant = userRepository.findById(complainantId)
                .orElseThrow(() -> new EntityNotFoundException("Complainant was not found: " + complainantId));
        User subject = userRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject was not found: " + subjectId));

        Complaint complaint = new Complaint();
        complaint.setComplainant(complainant);
        complaint.setSubject(subject);
        complaint.setCategory(category);
        complaint.setDescription(description);
        // These three columns are NOT NULL and record where the complaint came from. Default them
        // to an explicit "direct" provenance rather than leaving them null, which previously
        // surfaced as an opaque 409 from the database.
        complaint.setRelationshipType(defaultText(relationshipType, "DIRECT"));
        complaint.setSourceEntityType(defaultText(sourceEntityType, "DIRECT"));
        complaint.setSourceEntityId(defaultText(sourceEntityId, "DIRECT"));
        complaint.submit();

        Complaint saved = complaintRepository.save(complaint);
        eventRepository.save(ComplianceEvent.outcome(
                ComplianceEventType.COMPLAINT_SUBMITTED, null, subjectId));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Complaint> listComplaints(Pageable pageable) {
        return complaintRepository.findAll(pageable);
    }

    @Transactional
    public ComplianceCase openCase(UUID complaintId, UUID subjectId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Complaint was not found: " + complaintId));

        ComplianceCase complianceCase = new ComplianceCase();
        complianceCase.setComplaint(complaint);
        complianceCase.setSubject(userRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subject was not found: " + subjectId)));
        complianceCase.open();
        complaint.setStatus(com.wastina.trust.entity.ComplaintStatus.CONVERTED_TO_CASE);

        ComplianceCase saved = caseRepository.save(complianceCase);
        eventRepository.save(ComplianceEvent.outcome(ComplianceEventType.CASE_OPENED, saved, subjectId));
        return saved;
    }

    @Transactional
    public ComplianceCase startInvestigation(UUID caseId) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case was not found: " + caseId));
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
                .orElseThrow(() -> new EntityNotFoundException("Case was not found: " + caseId));
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
                .orElseThrow(() -> new EntityNotFoundException("Case was not found: " + caseId));

        // Only the subject of the case, or the complainant who raised it, may appeal.
        boolean isParty = complianceCase.getSubject() != null
                && complianceCase.getSubject().getId().equals(appellantId);
        if (!isParty && complianceCase.getComplaint() != null
                && complianceCase.getComplaint().getComplainant() != null) {
            isParty = complianceCase.getComplaint().getComplainant().getId().equals(appellantId);
        }
        if (!isParty) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "Only a party to this case may appeal its decision.");
        }

        complianceCase.markAppealed();
        Appeal appeal = new Appeal(complianceCase, appellantId, grounds);
        appeal.submit();
        Appeal saved = appealRepository.save(appeal);
        eventRepository.save(ComplianceEvent.outcome(
                ComplianceEventType.APPEAL_SUBMITTED, complianceCase, complianceCase.getSubject().getId()));
        return saved;
    }

    @Transactional
    public ComplianceAction applyTemporaryAction(UUID caseId, UUID actorId, ComplianceActionType type,
                                                 String reason, Instant expiresAt) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case was not found: " + caseId));

        ComplianceAction action = new ComplianceAction();
        action.setComplianceCase(complianceCase);
        action.setAppliedBy(actorId);
        action.setType(type);
        action.setReason(reason);
        action.setExpiresAt(expiresAt);
        action.activate();

        ComplianceAction saved = actionRepository.save(action);
        eventRepository.save(ComplianceEvent.outcome(
                ComplianceEventType.TEMPORARY_RESTRICTION_APPLIED, complianceCase,
                complianceCase.getSubject().getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public ComplianceCase getCase(UUID caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case was not found: " + caseId));
    }

    @Transactional(readOnly = true)
    public Page<ComplianceCase> listCases(Pageable pageable) {
        return caseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Appeal> listAppeals(UUID caseId) {
        return appealRepository.findAll().stream()
                .filter(appeal -> appeal.getComplianceCase() != null
                        && appeal.getComplianceCase().getId().equals(caseId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplianceAction> listActions(UUID caseId) {
        return actionRepository.findAll().stream()
                .filter(action -> action.getComplianceCase() != null
                        && action.getComplianceCase().getId().equals(caseId))
                .toList();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}