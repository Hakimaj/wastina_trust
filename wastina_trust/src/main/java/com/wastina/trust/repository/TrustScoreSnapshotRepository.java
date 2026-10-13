package com.wastina.trust.repository;

import com.wastina.trust.entity.TrustScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrustScoreSnapshotRepository extends JpaRepository<TrustScoreSnapshot, UUID> {
}