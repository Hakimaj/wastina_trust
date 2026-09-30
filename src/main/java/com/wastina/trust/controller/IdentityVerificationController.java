package com.wastina.trust.controller;

import com.wastina.trust.entity.IdentityVerification;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.IdentityVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identity-verifications")
@RequiredArgsConstructor
public class IdentityVerificationController {
    private final UserRepository userRepository;
    private final IdentityVerificationService verificationService;

    @PostMapping
    public ResponseEntity<?> start(@AuthenticationPrincipal Jwt jwt, @RequestBody StartVerificationRequest request) {
        UUID userId = userRepository.findByIdentitySubject(jwt.getSubject())
                .orElseThrow(() -> new IllegalStateException("IAM user mapping is missing"))
                .getId();
        IdentityVerification verification = verificationService.start(userId, request.purpose(), request.policyVersion(), request.locale());
        return ResponseEntity.accepted().body(Map.of("verification_id", verification.getId(), "status", verification.getStatus()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = userRepository.findByIdentitySubject(jwt.getSubject())
                .orElseThrow(() -> new IllegalStateException("IAM user mapping is missing"))
                .getId();
        return verificationService.findForUser(id, userId)
                .map(verification -> ResponseEntity.ok(Map.of(
                        "verification_id", verification.getId(),
                        "status", verification.getStatus(),
                        "identity_status", verification.getIdentityStatus())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record StartVerificationRequest(String purpose, String policyVersion, String locale) {
    }
}