package com.wastina.trust.controller;

import com.wastina.trust.dto.PaymentCreateRequest;
import com.wastina.trust.dto.PaymentView;
import com.wastina.trust.entity.Payment;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.PaymentRepository;
import com.wastina.trust.security.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import com.wastina.trust.web.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy payment records.
 *
 * <p>The audit found this endpoint family accepted an arbitrary {@code payerId}, so any
 * authenticated user could read any other user's payments. The payer is now always derived from the
 * JWT:
 * <ul>
 *   <li>{@code GET /api/v1/me/payments} lists the caller's own payments, paged.</li>
 *   <li>{@code GET /api/v1/me/payments/{id}} reads one of the caller's own payments.</li>
 *   <li>{@code GET /api/payments/{id}} remains for administrators only.</li>
 * </ul>
 */
@RestController
@Tag(name = "Payments", description = "Payment records")
public class PaymentController {

    private static final int MAX_PAGE_SIZE = 200;

    private final PaymentRepository paymentRepository;
    private final CurrentUserResolver currentUserResolver;

    public PaymentController(PaymentRepository paymentRepository, CurrentUserResolver currentUserResolver) {
        this.paymentRepository = paymentRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/v1/me/payments")
    @Transactional
    @Operation(summary = "Record a payment made by the caller")
    public ResponseEntity<PaymentView> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentCreateRequest request) {
        User payer = currentUserResolver.requireUser(jwt);

        Payment payment = new Payment();
        payment.setPayer(payer);
        payment.setAmount(request.amount());
        payment.setCurrency(normalizeCurrency(request.currency()));
        payment.setPurpose(request.purpose());
        payment.setStatus(com.wastina.trust.entity.EPaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());

        Payment saved = paymentRepository.save(payment);
        return ResponseEntity
                .created(URI.create("/api/v1/me/payments/" + saved.getId()))
                .body(PaymentView.from(saved));
    }

    @GetMapping("/api/v1/me/payments")
    @Operation(summary = "List the caller's payments (paged)")
    public PageResponse<PaymentView> listMyPayments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID payerId = currentUserResolver.requireUser(jwt).getId();
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        var result = paymentRepository.findByPayerId(
                payerId, PageRequest.of(Math.max(page, 0), pageSize));

        return PageResponse.of(result,
                result.getContent().stream().map(PaymentView::from).toList());
    }

    @GetMapping("/api/v1/me/payments/{id}")
    @Operation(summary = "Read one of the caller's payments")
    public PaymentView getOwnPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        UUID payerId = currentUserResolver.requireUser(jwt).getId();
        return PaymentView.from(requireOwnPayment(id, payerId));
    }

    /**
     * Administrator-only view of any payment. Restricted with an explicit authority because it is
     * the only remaining cross-account read in this domain.
     */
    @GetMapping("/api/payments/{id}")
    @PreAuthorize("hasAuthority('SCOPE_role:manage') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Read any payment (administrator only)")
    public ResponseEntity<PaymentView> getPaymentById(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(payment -> ResponseEntity.ok(PaymentView.from(payment)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Payment requireOwnPayment(Long id, UUID payerId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment was not found: " + id));
        if (payment.getPayer() == null || !payment.getPayer().getId().equals(payerId)) {
            throw new com.wastina.trust.controller.BusinessRuleViolationException(
                    ApiErrorCodes.FORBIDDEN, "You are not the payer of this payment.");
        }
        return payment;
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank()
                ? "ETB" : currency.trim().toUpperCase(Locale.ROOT);
    }

    }