package com.wastina.trust.service;

import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.entity.AccountStatus;
import com.wastina.trust.entity.ERole;
import com.wastina.trust.entity.Role;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.RoleRepository;
import com.wastina.trust.repository.UserRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves and persists {@link Role} rows and maps identity-provider roles onto a {@link User}.
 *
 * <p>Kept separate from the controller so the rule "a caller may only grant themselves
 * non-privileged roles" is enforced in one place and is unit-testable.
 */
@Service
public class RoleAssignmentService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleAssignmentService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /** Returns the persisted role, creating it on first use. */
    @Transactional
    public Role findOrCreate(ERole role) {
        return roleRepository.findByName(role).orElseGet(() -> roleRepository.save(new Role(role)));
    }

    /**
     * Resolves the roles a newly provisioned account should receive.
     *
     * <p>Privileged roles are filtered out rather than rejected, so a caller cannot infer which
     * roles exist or use a failed request as an oracle. Privileged roles are granted later by the
     * authorization server (Keycloak realm role) or by an operator, never by the signup payload.
     */
    @Transactional(readOnly = true)
    public Set<Role> resolveSelfServiceRoles(Collection<String> requestedRoles, List<String> tokenRoles) {
        Set<String> candidates = new LinkedHashSet<>();
        if (tokenRoles != null) {
            candidates.addAll(tokenRoles);
        }
        if (requestedRoles != null) {
            candidates.addAll(requestedRoles);
        }
        if (candidates.isEmpty()) {
            candidates.add(ERole.ROLE_WORKER.name());
        }

        Set<Role> roles = new LinkedHashSet<>();
        for (String candidate : candidates) {
            ERole role = parseRole(candidate);
            if (role != null && role.isSelfService()) {
                roles.add(findOrCreate(role));
            }
        }
        // A caller cannot end up with an account that has no role at all.
        if (roles.isEmpty()) {
            roles.add(findOrCreate(ERole.ROLE_WORKER));
        }
        return roles;
    }

    /** Maps token authorities ({@code ROLE_*} / bare role names) onto persisted {@link Role}s. */
    @Transactional
    public Set<Role> syncFromToken(User user, Collection<String> tokenRoles) {
        Set<Role> roles = new LinkedHashSet<>();
        if (tokenRoles == null || tokenRoles.isEmpty()) {
            return roles;
        }
        for (String candidate : tokenRoles) {
            ERole role = parseRole(candidate);
            if (role != null) {
                roles.add(findOrCreate(role));
            }
        }
        return roles;
    }

    public User requireBySubject(String identitySubject) {
        return userRepository.findByIdentitySubject(identitySubject)
                .orElseThrow(() -> new EntityNotFoundException("Account mapping was not found."));
    }

    public Optional<User> findBySubject(String identitySubject) {
        return userRepository.findByIdentitySubject(identitySubject);
    }

    /** Reports whether the account may still be used; a suspended account keeps its data. */
    public void requireUsable(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Account is not active (" + user.getAccountStatus() + ").");
        }
    }

    private ERole parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("SCOPE_")) {
            return null;
        }
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            return ERole.valueOf(normalized);
        } catch (IllegalArgumentException unknownRole) {
            return null;
        }
    }
}