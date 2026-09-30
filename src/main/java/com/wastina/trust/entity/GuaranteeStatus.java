package com.wastina.trust.entity;

import java.util.EnumSet;

public enum GuaranteeStatus {
    DRAFT,
    PENDING_ACCEPTANCE,
    ACCEPTED,
    ACTIVE,
    CLAIMED,
    FULFILLED,
    DEFAULTED,
    CANCELLED,
    EXPIRED,
    REJECTED;

    public boolean isExposureActive() {
        return EnumSet.of(ACCEPTED, ACTIVE, CLAIMED, DEFAULTED).contains(this);
    }
}
