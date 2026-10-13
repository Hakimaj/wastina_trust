package com.wastina.trust.service;

import com.wastina.trust.entity.Guarantee;
import com.wastina.trust.entity.GuaranteeStatus;
import com.wastina.trust.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GuaranteeCapacityServiceTest {
    private final GuaranteeCapacityService service = new GuaranteeCapacityService();

    @Test
    void concurrentCapacityRequestsAreBlockedWhenExposureLimitWouldBeExceeded() {
        User guarantor = new User("+251911000000");
        guarantor.setId(UUID.randomUUID());

        Guarantee existing = new Guarantee();
        existing.setGuarantor(guarantor);
        existing.setStatus(GuaranteeStatus.ACTIVE);
        existing.setOutstandingLiability(new BigDecimal("400000.00"));

        GuaranteeCapacityService.CapacityDecision decision = service.evaluate(guarantor, List.of(existing), new BigDecimal("150000.00"));

        assertFalse(decision.allowed());
        assertEquals(1L, decision.activeGuaranteeCount());
        assertTrue(decision.projectedLiability().compareTo(new BigDecimal("500000.00")) > 0);
    }

    @Test
    void activeGuaranteeStatusCountsTowardExposure() {
        User guarantor = new User("+251911000001");
        guarantor.setId(UUID.randomUUID());

        Guarantee accepted = new Guarantee();
        accepted.setGuarantor(guarantor);
        accepted.setStatus(GuaranteeStatus.ACCEPTED);
        accepted.setOutstandingLiability(new BigDecimal("25000.00"));

        GuaranteeCapacityService.CapacityDecision decision = service.evaluate(guarantor, List.of(accepted), new BigDecimal("1000.00"));

        assertTrue(decision.allowed());
        assertEquals(1L, decision.activeGuaranteeCount());
        assertEquals(new BigDecimal("25000.00"), decision.activeLiability());
    }
}
