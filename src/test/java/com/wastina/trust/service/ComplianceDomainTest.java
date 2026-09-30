package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceDomainTest {

    @Test
    void complaintSubmissionDoesNotRepresentAFinding() {
        Complaint complaint = new Complaint();
        complaint.setDescription("The reported transaction did not follow the agreed platform process.");
        complaint.submit();

        assertEquals(ComplaintStatus.SUBMITTED, complaint.getStatus());
        assertFalse(complaint.isFindingEstablished());
    }

    @Test
    void caseDecisionIsExplicitAndFinalDecisionCannotBeRewritten() {
        ComplianceCase complianceCase = new ComplianceCase();
        complianceCase.open();
        complianceCase.startInvestigation();
        complianceCase.beginDecision();
        complianceCase.decide(DecisionType.UPHELD);

        assertEquals(CaseStatus.DECIDED, complianceCase.getStatus());
        assertEquals(DecisionType.UPHELD, complianceCase.getDecisionType());
        assertThrows(IllegalStateException.class, () -> complianceCase.decide(DecisionType.DISMISSED));
    }

    @Test
    void appealOverturnsFindingWithoutDeletingOriginalDecision() {
        ComplianceCase complianceCase = new ComplianceCase();
        complianceCase.open();
        complianceCase.startInvestigation();
        complianceCase.beginDecision();
        complianceCase.decide(DecisionType.UPHELD);

        Appeal appeal = new Appeal(complianceCase, UUID.randomUUID(), AppealGrounds.NEW_EVIDENCE);
        appeal.submit();
        appeal.beginReview();
        appeal.resolve(AppealDecisionType.OVERTURNED);

        assertEquals(AppealStatus.RESOLVED, appeal.getStatus());
        assertEquals(AppealDecisionType.OVERTURNED, appeal.getDecisionType());
        assertEquals(DecisionType.UPHELD, complianceCase.getDecisionType());
    }

    @Test
    void temporaryActionRequiresExpiryOrReviewDate() {
        ComplianceAction action = new ComplianceAction();
        action.setType(ComplianceActionType.FEATURE_RESTRICTION);

        assertThrows(IllegalArgumentException.class, action::activate);
    }

    @Test
    void unauthorizedReviewerCannotDecideCase() {
        ComplianceAuthorizationService authorization = new ComplianceAuthorizationService();
        assertThrows(IllegalStateException.class,
                () -> authorization.requireDecisionAuthority(ComplianceRole.SUPPORT_AGENT, false));
        assertDoesNotThrow(() -> authorization.requireDecisionAuthority(ComplianceRole.COMPLIANCE_MANAGER, true));
    }
}
