package com.wastina.trust.repository;

import com.wastina.trust.entity.TrustEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrustEventRepository extends JpaRepository<TrustEvent, UUID> {
    List<TrustEvent> findBySubjectUserIdOrderByEffectiveAtAsc(UUID subjectUserId);
}