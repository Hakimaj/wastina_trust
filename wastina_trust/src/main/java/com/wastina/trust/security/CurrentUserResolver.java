package com.wastina.trust.security;

import com.wastina.trust.entity.User;
import com.wastina.trust.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class CurrentUserResolver {
    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required.");
        }
        return userRepository.findByIdentitySubject(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account mapping was not found."));
    }

    public UUID requireMatchingUserId(Jwt jwt, UUID requestedUserId) {
        UUID currentUserId = requireUser(jwt).getId();
        if (!currentUserId.equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The requested account does not match the authenticated user.");
        }
        return currentUserId;
    }
}