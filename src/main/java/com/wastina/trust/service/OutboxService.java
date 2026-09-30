package com.wastina.trust.service;

import com.wastina.trust.entity.OutboxEvent;
import com.wastina.trust.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository repository;

    @Transactional
    public OutboxEvent enqueue(String eventType, String sourceService, String aggregateType,
                               String aggregateId, UUID subjectUserId, String payloadJson,
                               String traceId, String correlationId) {
        if (eventType == null || eventType.isBlank() || payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("Event type and payload are required.");
        }
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setSourceService(sourceService);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setSubjectUserId(subjectUserId);
        event.setPayloadJson(payloadJson);
        event.setTraceId(traceId);
        event.setCorrelationId(correlationId);
        return repository.save(event);
    }
}
