package com.wastina.trust.controller;

import com.wastina.trust.dto.IdentityVerificationView;
import com.wastina.trust.security.CurrentUserResolver;
import com.wastina.trust.service.IdentityVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identity verification.
 *
 * <p>Completes the lifecycle the audit found broken. {@code POST /} now returns the provider
 * redirect URL and an {@code actionRequired} flag so a client can send the user onward, and
 * {@code POST /{id}/complete} resolves the attempt with the provider.
 *
 * <p>The completion endpoint is owner-scoped: it resolves the attempt belonging to the caller only.
 * A provider-driven callback is a separate concern and should be wired to a webhook authenticated by
 * the provider, not to this endpoint.
 */
@RestController
@RequestMapping("/api/v1/identity-verifications")
@Tag(name = "Identity", description = "Identity verification")
public class IdentityVerificationController {

    private final IdentityVerificationService verificationService;
    private final CurrentUserResolver currentUserResolver;

    public IdentityVerificationController(IdentityVerificationService verificationService,
                                          CurrentUserResolver currentUserResolver) {
        this.verificationService = verificationService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List the caller's identity verification attempts")
    public List<IdentityVerificationView> listCurrentUserVerifications(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        return verificationService.listForUser(userId).stream()
                .map(verification -> IdentityVerificationView.from(verification, null))
                .toList();
    }

    @PostMapping
    @Operation(summary = "Start identity verification and return where to continue")
    public IdentityVerificationView start(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StartVerificationRequest request) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        var result = verificationService.start(
                userId, request.purpose(), request.policyVersion(), request.locale());
        return IdentityVerificationView.from(result.verification(), result.redirectUrl());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Read one of the caller's verification attempts")
    public IdentityVerificationView get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        return verificationService.findForUser(id, userId)
                .map(verification -> IdentityVerificationView.from(verification, null))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Identity verification was not found: " + id));
    }

    /**
     * Completes the caller's outstanding verification attempt.
     *
     * <p>This is the step that was missing: nothing previously ever asked the provider for a result,
     * so {@code PENDING} could never become {@code SUCCEEDED}.
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete an outstanding verification attempt with the provider")
    public IdentityVerificationView complete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = currentUserResolver.requireUser(jwt).getId();
        return IdentityVerificationView.from(
                verificationService.resolveForUser(id, userId), null);
    }

    public record StartVerificationRequest(
            @NotBlank(message = "is required")
            @Size(max = 100, message = "must be at most 100 characters")
            String purpose,

            @NotBlank(message = "is required")
            @Size(max = 100, message = "must be at most 100 characters")
            String policyVersion,

            @NotBlank(message = "is required")
            @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "must be a BCP-47 locale such as 'en' or 'am-ET'")
            String locale) {
    }
}