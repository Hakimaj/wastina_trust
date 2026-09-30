package com.wastina.trust.controller;

import com.wastina.trust.entity.PaymentProviderType;
import com.wastina.trust.entity.PaymentWebhook;
import com.wastina.trust.service.PaymentGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PaymentWebhookController {
    private final PaymentGatewayService paymentGatewayService;

    public PaymentWebhookController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/webhooks/payment")
    public ResponseEntity<PaymentWebhook> handleWebhook(
            @RequestParam PaymentProviderType provider,
            @RequestHeader("X-Provider-Event-Id") String providerEventId,
            @RequestHeader(value = "X-Provider-Transaction-Id", required = false) String providerTransactionId,
            @RequestHeader(value = "X-Provider-Signature", required = false) String signature,
            @RequestBody String rawPayload) {
        return ResponseEntity.ok(paymentGatewayService.receiveWebhook(provider, providerEventId,
                providerTransactionId, rawPayload, signature));
    }
}
