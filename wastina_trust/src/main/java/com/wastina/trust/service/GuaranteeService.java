package com.wastina.trust.service;

import com.wastina.trust.controller.ApiErrorCodes;
import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.dto.GuaranteeCreateRequest;
import com.wastina.trust.entity.Guarantee;
import com.wastina.trust.entity.GuaranteeStatus;
import com.wastina.trust.entity.GuaranteeType;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.GuaranteeRepository;
import com.wastina.trust.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarantee creation and lifecycle.
 *
 * <p>The audit found {@code GuaranteeController} built a {@link Guarantee}, never saved it, and
 * returned {@code 200 OK} with a fabricated {@code WST-<epochMillis>} identifier that resolved to
 * nothing. It also took the principal from the request body, so any caller could create a guarantee
 * in another party's name.
 *
 * <p>This service persists the entity, derives the principal from the authenticated caller,
 * generates a real unique public reference, and enforces guarantor capacity from the guarantor's
 * actual outstanding liability rather than an empty list.
 */
@Service
public class GuaranteeService {

    private static final int DEFAULT_VALIDITY_DAYS = 30;
    private static final String REFERENCE_PREFIX = "WST-";

    private final GuaranteeRepository guaranteeRepository;
    private final UserRepository userRepository;
    private final GuaranteeCapacityService guaranteeCapacityService;
    private final ContractIntegrityService contractIntegrityService;

    public GuaranteeService(GuaranteeRepository guaranteeRepository,
                            UserRepository userRepository,
                            GuaranteeCapacityService guaranteeCapacityService,
                            ContractIntegrityService contractIntegrityService) {
        this.guaranteeRepository = guaranteeRepository;
        this.userRepository = userRepository;
        this.guaranteeCapacityService = guaranteeCapacityService;
        this.contractIntegrityService = contractIntegrityService;
    }

    @Transactional
    public Guarantee createGuarantee(User principal, GuaranteeCreateRequest request) {
        User guarantor = userRepository.findById(request.guarantorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Guarantor was not found: " + request.guarantorId()));

        if (guarantor.getId().equals(principal.getId())) {
            throw new IllegalArgumentException("A user cannot be their own guarantor.");
        }

        User beneficiary = null;
        if (request.beneficiaryId() != null) {
            beneficiary = userRepository.findById(request.beneficiaryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Beneficiary was not found: " + request.beneficiaryId()));
        }

        BigDecimal amount = request.amount();
        String currency = normalizeCurrency(request.currency());

        // Capacity is evaluated against the guarantor's real outstanding exposure.
        List<Guarantee> guarantorGuarantees = guaranteeRepository.findAllByGuarantorAndStatusIn(
                guarantor, List.of(GuaranteeStatus.ACCEPTED, GuaranteeStatus.ACTIVE,
                        GuaranteeStatus.CLAIMED, GuaranteeStatus.DEFAULTED));
        var capacity = guaranteeCapacityService.evaluate(guarantor, guarantorGuarantees, amount);
        if (!capacity.allowed()) {
            throw new BusinessRuleViolationException(
                    ApiErrorCodes.INSUFFICIENT_GUARANTEE_CAPACITY,
                    "Guarantor capacity exceeded: " + capacity.activeGuaranteeCount() + " of "
                            + capacity.maxActiveGuarantees() + " guarantees in use, projected liability "
                            + capacity.projectedLiability() + " " + currency + " against a limit of "
                            + capacity.maxActiveLiability() + ".");
        }

        Guarantee guarantee = new Guarantee();
        guarantee.setPublicReference(generateReference());
        guarantee.setType(request.type() == null ? GuaranteeType.OTHER : request.type());
        guarantee.setStatus(GuaranteeStatus.PENDING_ACCEPTANCE);
        guarantee.setPrincipal(principal);
        guarantee.setGuarantor(guarantor);
        guarantee.setBeneficiary(beneficiary);
        guarantee.setAmount(amount);
        guarantee.setCurrency(currency);
        guarantee.setGuaranteedAmount(amount);
        guarantee.setOutstandingLiability(amount);
        guarantee.setPurpose(defaultText(request.purpose(), "Guarantee"));
        guarantee.setDescription(request.description() == null ? "" : request.description());
        guarantee.setExpiresAt(Instant.now().plus(DEFAULT_VALIDITY_DAYS, ChronoUnit.DAYS));

        // Hash the canonical contract terms so the signed artifact is tamper-evident.
        Map<String, Object> contract = new java.util.LinkedHashMap<>();
        contract.put("principalId", principal.getId().toString());
        contract.put("guarantorId", guarantor.getId().toString());
        contract.put("beneficiaryId", beneficiary == null ? null : beneficiary.getId().toString());
        contract.put("amount", amount.toPlainString());
        contract.put("currency", currency);
        contract.put("purpose", guarantee.getPurpose());
        contract.put("status", guarantee.getStatus().name());
        contract.put("termsVersion", guarantee.getTermsVersion());
        contract.put("contractVersion", guarantee.getContractVersion());
        guarantee.setContractHash(
                contractIntegrityService.sha256Hex(contractIntegrityService.canonicalize(contract)));

        return guaranteeRepository.save(guarantee);
    }

    /**
     * Accepts a pending guarantee. Only the guarantor may accept, since accepting is what creates
     * the exposure against their capacity.
     */
    @Transactional
    public Guarantee acceptGuarantee(String reference, User actor) {
        Guarantee guarantee = requireVisibleGuarantee(reference, actor);
        if (!guarantee.getGuarantor().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the guarantor can accept this guarantee.");
        }
        if (guarantee.getStatus() != GuaranteeStatus.PENDING_ACCEPTANCE
                && guarantee.getStatus() != GuaranteeStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                    "Only a pending guarantee can be accepted. Current status: " + guarantee.getStatus() + ".");
        }
        guarantee.accept();
        return guaranteeRepository.save(guarantee);
    }

    /** Rejects a pending guarantee. Only the guarantor may reject. */
    @Transactional
    public Guarantee rejectGuarantee(String reference, User actor, String reason) {
        Guarantee guarantee = requireVisibleGuarantee(reference, actor);
        if (!guarantee.getGuarantor().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the guarantor can reject this guarantee.");
        }
        if (guarantee.getStatus() != GuaranteeStatus.PENDING_ACCEPTANCE
                && guarantee.getStatus() != GuaranteeStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                    "Only a pending guarantee can be rejected. Current status: " + guarantee.getStatus() + ".");
        }
        guarantee.setStatus(GuaranteeStatus.REJECTED);
        if (reason != null && !reason.isBlank()) {
            guarantee.setDescription(guarantee.getDescription() + "\nRejected: " + reason.trim());
        }
        return guaranteeRepository.save(guarantee);
    }

    @Transactional(readOnly = true)
    public List<Guarantee> listForUser(UUID userId) {
        return guaranteeRepository.findByPrincipal(userId,
                org.springframework.data.domain.PageRequest.of(0, 200)).getContent();
    }

    @Transactional(readOnly = true)
    public Guarantee getForUser(String reference, User actor) {
        return requireVisibleGuarantee(reference, actor);
    }

    /**
     * A guarantee is visible to its principal, its guarantor and its beneficiary, and to nobody
     * else. Lookup accepts either the public reference ({@code WST-XXXXXXXX}) or the internal
     * numeric id, and returns {@code 404} when neither matches so existence is not leaked.
     */
    @Transactional(readOnly = true)
    public Guarantee requireVisibleGuarantee(String reference, User actor) {
        Guarantee guarantee = guaranteeRepository.findByPublicReference(reference)
                .or(() -> parseNumericId(reference)
                        .flatMap(guaranteeRepository::findById))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Guarantee was not found: " + reference));

        boolean party = guarantee.getPrincipal().getId().equals(actor.getId())
                || guarantee.getGuarantor().getId().equals(actor.getId())
                || (guarantee.getBeneficiary() != null
                && guarantee.getBeneficiary().getId().equals(actor.getId()));

        if (!party) {
            throw new AccessDeniedException("You are not a party to this guarantee.");
        }
        return guarantee;
    }

    private java.util.Optional<Long> parseNumericId(String reference) {
        try {
            return java.util.Optional.of(Long.parseLong(reference.trim()));
        } catch (NumberFormatException notNumeric) {
            return java.util.Optional.empty();
        }
    }

    private String generateReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = REFERENCE_PREFIX + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            if (guaranteeRepository.findByPublicReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique guarantee reference.");
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "ETB";
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code.");
        }
        return normalized;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}