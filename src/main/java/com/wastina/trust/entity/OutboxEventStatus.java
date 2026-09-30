package com.wastina.trust.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD_LETTERED
}
