package com.wastina.trust.security;

import com.wastina.trust.entity.AccountStatus;
import com.wastina.trust.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            boolean active = userRepository.findByIdentitySubject(jwt.getSubject())
                    .map(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                    .orElse(false);
            if (!active) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is not active");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}