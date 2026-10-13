package com.wastina.trust.security;

import com.wastina.trust.entity.User;
import com.wastina.trust.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserResolverTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserResolver resolver = new CurrentUserResolver(userRepository);
    private final UUID accountId = UUID.randomUUID();
    private final Jwt jwt = Jwt.withTokenValue("local")
            .header("alg", "none")
            .subject("local-subject")
            .build();

    @Test
    void resolvesOnlyTheAccountMappedToTheJwtSubject() {
        User user = new User("+251900000001");
        user.setId(accountId);
        user.setIdentitySubject("local-subject");
        when(userRepository.findByIdentitySubject("local-subject")).thenReturn(Optional.of(user));

        assertEquals(accountId, resolver.requireMatchingUserId(jwt, accountId));
    }

    @Test
    void rejectsAClientSuppliedDifferentAccountId() {
        User user = new User("+251900000001");
        user.setId(accountId);
        user.setIdentitySubject("local-subject");
        when(userRepository.findByIdentitySubject("local-subject")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.requireMatchingUserId(jwt, UUID.randomUUID()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void reportsMissingAccountMappingAsNotFound() {
        when(userRepository.findByIdentitySubject("local-subject")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.requireUser(jwt));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rejectsMissingAuthentication() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resolver.requireUser(null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}