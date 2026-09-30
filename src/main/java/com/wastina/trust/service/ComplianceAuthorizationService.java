package com.wastina.trust.service;

import com.wastina.trust.entity.ComplianceRole;
import org.springframework.stereotype.Service;

@Service
public class ComplianceAuthorizationService {

    public void requireDecisionAuthority(ComplianceRole role, boolean caseScopeGranted) {
        if (!caseScopeGranted || (role != ComplianceRole.CASE_REVIEWER && role != ComplianceRole.COMPLIANCE_MANAGER && role != ComplianceRole.ADMIN)) {
            throw new IllegalStateException("Decision authority is not granted for this case.");
        }
    }

    public void requireAppealAuthority(ComplianceRole role, boolean separateReviewer, boolean caseScopeGranted) {
        if (!caseScopeGranted || !separateReviewer || (role != ComplianceRole.COMPLIANCE_MANAGER && role != ComplianceRole.ADMIN)) {
            throw new IllegalStateException("Appeal authority is not granted for this case.");
        }
    }

    public void requireEvidenceAccess(ComplianceRole role, boolean caseScopeGranted) {
        if (!caseScopeGranted || role == ComplianceRole.SUPPORT_AGENT) {
            throw new IllegalStateException("Evidence access is not granted.");
        }
    }
}
