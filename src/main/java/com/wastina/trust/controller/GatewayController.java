package com.wastina.trust.controller;

import com.wastina.trust.entity.PaymentIntent;
import com.wastina.trust.entity.PaymentProviderType;
import com.wastina.trust.entity.PaymentTransaction;
import com.wastina.trust.dto.PaymentIntentView;
import com.wastina.trust.dto.PaymentTransactionView;
import com.wastina.trust.entity.User;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment intent and transaction endpoints.
 *
 * <p>The payer is derived from the JWT (previously supplied in the body, so any caller could open
 * an intent against another account), and responses are explicit view DTOs rather than raw JPA
 * entities.
 *
 * <p>Idempotency is preserved: a repeated {@code idempotencyKey} with the same amount and currency
 * returns the existing intent; the same key with different terms is refused.
 *
 * <p>Payment provider webhooks are intentionally <em>not</em> routed here — see
 * {@link PaymentWebhookController}.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment intents and transactions")
public class GatewayController {

    private final PaymentGatewayService paymentGatewayService;
    private final CurrentUserResolver currentUserResolver;

    public GatewayController(PaymentGatewayService paymentGatewayService,
                             CurrentUserResolver currentUserResolver) {
        this.paymentGatewayService = paymentGatewayService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/intents")
    @Operation(summary = "Create a payment intent for the caller")
    public PaymentIntentView createPaymentIntent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentIntentRequest request) {
        User payer = currentUserResolver.requireUser(jwt);
        PaymentIntent intent = paymentGatewayService.createIntent(
                payer.getId(), request.amount(), request.currency(), request.provider(),
                request.purpose(), request.idempotencyKey());
        return PaymentIntentView.from(intent);
    }

    @PostMapping("/intents/{intentId}/initialize")
    @Operation(summary = "Initialize a provider transaction for an intent")
    public PaymentTransactionView initializePayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long intentId) {
        UUID payerId = currentUserResolver.requireUser(jwt).getId();
        return PaymentTransactionView.from(
                paymentGatewayService.initializeTransactionForPayer(intentId, payerId));
    }

    @PostMapping("/transactions/{transactionId}/verify")
    @Operation(summary = "Verify a transaction against the provider")
    public PaymentTransactionView verifyPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID transactionId) {
        UUID payerId = currentUserResolver.requireUser(jwt).getId();
        return PaymentTransactionView.from(
                paymentGatewayService.verifyTransactionForPayer(transactionId, payerId));
    }

    public record PaymentIntentRequest(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Amount to charge",
                    example = "1500.00", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
            @NotNull(message = "is required")
            @DecimalMin(value = "0.01", message = "must be greater than zero")
            @Digits(integer = 16, fraction = 2, message = "must have at most 16 integer and 2 decimal digits")
            BigDecimal amount,

            @NotBlank(message = "is required")
            @Pattern(regexp = "[A-Za-z]{3}", message = "must be a 3-letter ISO currency code")
            String currency,

            @NotNull(message = "is required")
            PaymentProviderType provider,

            @Size(max = 120, message = "must be at most 120 characters")
            String purpose,

            @NotBlank(message = "is required")
            @Size(min = 8, max = 120, message = "must be between 8 and 120 characters")
            String idempotencyKey) {
    }
}