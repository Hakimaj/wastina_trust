package com.wastina.trust.entity;

public enum PaymentTransactionStatus {
    CREATED,
    PROCESSING,
    SUCCESSFUL,
    FAILED,
    UNKNOWN,
    RECONCILIATION_REQUIRED,
    REVERSED,
    REFUND_PENDING,
    REFUNDED
}
