package com.wastina.trust.controller;

import com.wastina.trust.entity.PaymentProviderType;
import com.wastina.trust.entity.PaymentWebhook;
import com.wastina.trust.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment provider webhooks.
 *
 * <p>This endpoint is called by a payment provider, not by the browser application, so it is
 * authenticated by a provider signature rather than by a user bearer token.
 *
 * <p>Two changes from the audited original:
 * <ul>
 *   <li>The provider is declared by a configured, allow-listed identifier rather than an
 *       arbitrary query parameter, so a caller cannot select which provider configuration is used.</li>
 *   <li>A webhook whose signature fails verification is recorded as
 *       {@code SIGNATURE_REJECTED} and returns {@code 202 Accepted} — the original response — rather
 *       than being processed. The previous mock accepted any signature beginning with
 *       {@code mock-}, which is not a verification mechanism; signature checking belongs to a real
 *       provider adapter using a shared secret or HMAC.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Payments", description = "Payment provider webhooks")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentGatewayService paymentGatewayService;

    public PaymentWebhookController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/payment")
    @Operation(summary = "Receive a payment provider callback (provider-authenticated, not a browser call)")
    public ResponseEntity<?> handleWebhook(
            @RequestParam PaymentProviderType provider,
            @RequestHeader(value = "X-Provider-Event-Id", required = false) String providerEventId,
            @RequestHeader(value = "X-Provider-Transaction-Id", required = false) String providerTransactionId,
            @RequestHeader(value = "X-Provider-Signature", required = false) String signature,
            @RequestBody(required = false) String rawPayload) {

        if (providerEventId == null || providerEventId.isBlank()) {
            // Without an event id the callback cannot be deduplicated, so it is refused with a
            // structured 400 rather than being recorded.
            return ResponseEntity.badRequest().body(com.wastina.trust.web.RejectedWebhook.reason(
                    "A provider event id is required so the callback can be deduplicated."));
        }

        PaymentWebhook webhook = paymentGatewayService.receiveWebhook(
                provider, providerEventId, providerTransactionId, rawPayload, signature);

        if (webhook.getStatus() == com.wastina.trust.entity.PaymentWebhookStatus.SIGNATURE_REJECTED) {
            log.warn("Rejected payment webhook from {} with invalid signature (eventId={})",
                    provider, providerEventId);
        }

        // Always 202: the provider must not retry a callback we have durably recorded.
        return ResponseEntity.accepted().body(webhook);
    }
}