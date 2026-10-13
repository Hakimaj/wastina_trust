package com.wastina.trust.service;

import com.wastina.trust.entity.OutboxEvent;
import com.wastina.trust.entity.OutboxEventStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxEventTest {
    @Test
    void failedPublicationRetriesThenDeadLettersAtConfiguredLimit() {
        OutboxEvent event = new OutboxEvent();

        event.recordFailure("broker unavailable", 2);
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        event.recordFailure("broker unavailable", 2);
        assertEquals(OutboxEventStatus.DEAD_LETTERED, event.getStatus());
        assertEquals(2, event.getAttemptCount());
    }

    @Test
    void publishedEventIsTerminalAndIdempotent() {
        OutboxEvent event = new OutboxEvent();
        event.markPublished();
        event.markPublished();

        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
    }
}
