package com.wastina.trust.repository;

import com.wastina.trust.entity.TrustScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrustScoreHistoryRepository extends JpaRepository<TrustScoreHistory, UUID> {
    List<TrustScoreHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}