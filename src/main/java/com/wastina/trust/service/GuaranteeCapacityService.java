package com.wastina.trust.service;

import com.wastina.trust.entity.Guarantee;
import com.wastina.trust.entity.GuaranteeStatus;
import com.wastina.trust.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GuaranteeCapacityService {
    private static final int MAX_ACTIVE_GUARANTEES = 5;
    private static final BigDecimal MAX_ACTIVE_LIABILITY = new BigDecimal("500000.00");

    public CapacityDecision evaluate(User guarantor, List<Guarantee> guarantees, BigDecimal requestedLiability) {
        long activeGuaranteeCount = guarantees.stream()
                .filter(g -> g.getGuarantor() != null && g.getGuarantor().getId().equals(guarantor.getId()))
                .filter(g -> g.getStatus() != null)
                .filter(g -> g.getStatus().isExposureActive())
                .count();

        BigDecimal activeLiability = guarantees.stream()
                .filter(g -> g.getGuarantor() != null && g.getGuarantor().getId().equals(guarantor.getId()))
                .filter(g -> g.getStatus() != null)
                .filter(g -> g.getStatus().isExposureActive())
                .map(Guarantee::getOutstandingLiability)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nextLiability = activeLiability.add(requestedLiability == null ? BigDecimal.ZERO : requestedLiability);
        boolean allowed = activeGuaranteeCount < MAX_ACTIVE_GUARANTEES && nextLiability.compareTo(MAX_ACTIVE_LIABILITY) <= 0;

        return new CapacityDecision(allowed, activeGuaranteeCount, activeLiability, nextLiability, MAX_ACTIVE_GUARANTEES, MAX_ACTIVE_LIABILITY);
    }

    public record CapacityDecision(
            boolean allowed,
            long activeGuaranteeCount,
            BigDecimal activeLiability,
            BigDecimal projectedLiability,
            long maxActiveGuarantees,
            BigDecimal maxActiveLiability
    ) {}
}
