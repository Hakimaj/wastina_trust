package com.wastina.trust.entity;

public enum PaymentWebhookStatus {
    RECEIVED,
    SIGNATURE_REJECTED,
    PENDING,
    PROCESSED,
    DUPLICATE,
    FAILED
}
