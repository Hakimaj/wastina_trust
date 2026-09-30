package com.wastina.trust.repository;

import com.wastina.trust.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
}
