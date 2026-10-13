package com.wastina.trust.repository;

import com.wastina.trust.entity.OutboxEvent;
import com.wastina.trust.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List<OutboxEventStatus> statuses, Instant now);
}
