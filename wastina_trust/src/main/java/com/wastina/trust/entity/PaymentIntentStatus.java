package com.wastina.trust.entity;

public enum PaymentIntentStatus {
    CREATED,
    PENDING_PROVIDER,
    PENDING_USER,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED,
    VERIFICATION_REQUIRED,
    REFUND_PENDING,
    REFUNDED,
    REVERSED,
    DISPUTED
}
