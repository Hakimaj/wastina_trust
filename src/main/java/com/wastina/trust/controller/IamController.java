package com.wastina.trust.controller;

import com.wastina.trust.entity.User;
import com.wastina.trust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class IamController {
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal Jwt jwt) {
        return userRepository.findByIdentitySubject(jwt.getSubject())
                .map(user -> ResponseEntity.ok(toView(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping
    @SuppressWarnings("null")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ProfileUpdateRequest request) {
        User user = userRepository.findByIdentitySubject(jwt.getSubject())
                .orElseThrow(() -> new IllegalStateException("IAM user mapping is missing"));
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        return ResponseEntity.ok(toView(userRepository.save(user)));
    }

    private Map<String, Object> toView(User user) {
        return Map.of(
                "id", user.getId(),
                "account_status", user.getAccountStatus(),
                "identity_status", user.getIdentityStatus(),
                "roles", user.getRoles().stream().map(role -> role.getName().name()).toList());
    }

    public record ProfileUpdateRequest(String email, String phoneNumber) {
    }
}