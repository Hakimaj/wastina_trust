package com.wastina.trust.controller;

import com.wastina.trust.entity.*;
import com.wastina.trust.service.DocumentGatewayService;
import com.wastina.trust.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GatewayController {
    private final DocumentGatewayService documentGatewayService;
    private final PaymentGatewayService paymentGatewayService;

    @PostMapping("/documents/upload-sessions")
    public ResponseEntity<DocumentUploadSession> createUploadSession(@RequestBody DocumentUploadRequest request) {
        return ResponseEntity.ok(documentGatewayService.createUploadSession(request.ownerUserId(), request.type(),
                request.purpose(), request.declaredMimeType()));
    }

    @PostMapping("/documents/upload-sessions/{sessionId}/complete")
    public ResponseEntity<Document> completeUpload(@PathVariable UUID sessionId,
                                                   @RequestBody byte[] content,
                                                   @RequestHeader("X-Detected-Mime-Type") String detectedMimeType) {
        return ResponseEntity.ok(documentGatewayService.completeUpload(sessionId, content, detectedMimeType));
    }

    @GetMapping("/documents/{documentId}/download-url")
    public ResponseEntity<String> downloadUrl(@PathVariable UUID documentId,
                                              @RequestParam UUID requesterId,
                                              @RequestParam boolean authorized) {
        return ResponseEntity.ok(documentGatewayService.issueDownloadUrl(documentId, requesterId, authorized));
    }

    @PostMapping("/payments/intents")
    public ResponseEntity<PaymentIntent> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        return ResponseEntity.ok(paymentGatewayService.createIntent(request.payerId(), request.amount(), request.currency(),
                request.provider(), request.purpose(), request.idempotencyKey()));
    }

    @PostMapping("/payments/intents/{intentId}/initialize")
    public ResponseEntity<PaymentTransaction> initializePayment(@PathVariable Long intentId) {
        return ResponseEntity.ok(paymentGatewayService.initializeTransaction(intentId));
    }

    @PostMapping("/payments/transactions/{transactionId}/verify")
    public ResponseEntity<PaymentTransaction> verifyPayment(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(paymentGatewayService.verifyTransaction(transactionId));
    }

    public record DocumentUploadRequest(UUID ownerUserId, DocumentType type, String purpose, String declaredMimeType) {}

    public record PaymentIntentRequest(UUID payerId, BigDecimal amount, String currency,
                                      PaymentProviderType provider, String purpose, String idempotencyKey) {}
}
