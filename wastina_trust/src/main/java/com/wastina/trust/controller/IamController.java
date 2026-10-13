package com.wastina.trust.controller;

import com.wastina.trust.dto.CurrentUserView;
import com.wastina.trust.entity.AccountStatus;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.service.RoleAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM endpoints for the authenticated account: read, provision and update.
 *
 * <p>Changes from the audited original:
 * <ul>
 *   <li>The response includes {@code email} and {@code phoneNumber} (previously absent), plus
 *       account metadata, and keeps the four legacy keys so existing consumers keep working.</li>
 *   <li>Provisioning now persists roles. Previously a new user was created with none, so
 *       role-gated navigation and routes collapsed for every real user.</li>
 *   <li>A caller cannot grant themselves a privileged role; only {@code ROLE_WORKER},
 *       {@code ROLE_GUARANTOR} and {@code ROLE_EMPLOYER} are self-service.</li>
 *   <li>Requests are validated, so a malformed email is a {@code 400} instead of an opaque
 *       {@code 500} from the database.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "IAM", description = "Current account")
public class IamController {

    private final UserRepository userRepository;
    private final RoleAssignmentService roleAssignmentService;

    public IamController(UserRepository userRepository, RoleAssignmentService roleAssignmentService) {
        this.userRepository = userRepository;
        this.roleAssignmentService = roleAssignmentService;
    }

    @GetMapping
    @Operation(summary = "Read the authenticated account")
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal Jwt jwt) {
        return userRepository.findByIdentitySubject(jwt.getSubject())
                .map(CurrentUserView::from)
                .map(view -> ResponseEntity.ok(mergedView(view)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/provision")
    @Transactional
    @Operation(summary = "Create or complete the local Wastina account for the authenticated identity")
    public ResponseEntity<?> provisionCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication,
            @Valid @RequestBody ProvisionRequest request) {
        User user = userRepository.findByIdentitySubject(jwt.getSubject())
                .orElseGet(() -> createUser(jwt, request));

        if (isBlank(user.getEmail())) {
            String email = firstNonBlank(request.email(), jwt.getClaimAsString("email"));
            if (email == null) {
                throw new BusinessRuleViolationException(
                        "An email address is required to complete provisioning.");
            }
            user.setEmail(email);
        }
        if (isBlank(user.getPhoneNumber())) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (user.getAccountStatus() == null) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }

        // Grant the persisted roles the account is entitled to. The database is the source of
        // truth for route access and navigation, so this must not be left empty.
        var roles = roleAssignmentService.resolveSelfServiceRoles(
                request.roles(), authorityNames(authentication));
        user.getRoles().addAll(roles);

        return ResponseEntity.ok(mergedView(CurrentUserView.from(userRepository.save(user))));
    }

    @PatchMapping
    @Transactional
    @Operation(summary = "Update the authenticated account's contact details")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateRequest request) {
        User user = userRepository.findByIdentitySubject(jwt.getSubject())
                .orElseThrow(() -> new EntityNotFoundException("IAM user mapping is missing"));

        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        // Return the persisted representation so the change can be confirmed by the caller and by
        // a follow-up read, which the original projection made impossible.
        return ResponseEntity.ok(mergedView(CurrentUserView.from(userRepository.save(user))));
    }

    private User createUser(Jwt jwt, ProvisionRequest request) {
        User user = new User(request.phoneNumber());
        user.setId(UUID.randomUUID());
        user.setEmail(firstNonBlank(request.email(), jwt.getClaimAsString("email")));
        user.setIdentitySubject(jwt.getSubject());
        user.setAccountStatus(AccountStatus.ACTIVE);
        return user;
    }

    /**
     * Emits the new representation plus the four legacy keys so a frontend written against the
     * previous contract continues to work without modification.
     */
    private java.util.Map<String, Object> mergedView(CurrentUserView view) {
        java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>(view.toLegacyMap());
        merged.put("identitySubject", view.identitySubject());
        merged.put("email", view.email());
        merged.put("phoneNumber", view.phoneNumber());
        merged.put("accountStatus", view.accountStatus());
        merged.put("identityStatus", view.identityStatus());
        merged.put("enabled", view.enabled());
        merged.put("verifiedAt", view.verifiedAt());
        merged.put("createdAt", view.createdAt());
        merged.put("updatedAt", view.updatedAt());
        return merged;
    }

    private List<String> authorityNames(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(name -> name != null && name.startsWith("ROLE_"))
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return !isBlank(preferred) ? preferred.trim() : (!isBlank(fallback) ? fallback.trim() : null);
    }

    public record ProfileUpdateRequest(
            @Email(message = "must be a valid email address")
            @Size(max = 320, message = "must be at most 320 characters")
            String email,

            @Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$",
                    message = "must be 7-32 characters using digits, spaces, +, ( ) or -")
            String phoneNumber) {
    }

    public record ProvisionRequest(
            @Email(message = "must be a valid email address")
            @Size(max = 320, message = "must be at most 320 characters")
            String email,

            @NotBlank(message = "is required")
            @Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$",
                    message = "must be 7-32 characters using digits, spaces, +, ( ) or -")
            String phoneNumber,

            @Size(max = 8, message = "must contain at most 8 role names")
            List<@Pattern(regexp = "^(?i)ROLE_[A-Z_]+$",
                    message = "must be a ROLE_ prefixed role name") String> roles) {
    }
}