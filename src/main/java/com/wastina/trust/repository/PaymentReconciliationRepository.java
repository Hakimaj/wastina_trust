package com.wastina.trust.repository;

import com.wastina.trust.entity.PaymentReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentReconciliationRepository extends JpaRepository<PaymentReconciliation, UUID> {
}
