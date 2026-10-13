package com.wastina.trust.config;

import com.wastina.trust.entity.AccountStatus;
import com.wastina.trust.entity.EmployerProfile;
import com.wastina.trust.entity.ERole;
import com.wastina.trust.entity.GuarantorProfile;
import com.wastina.trust.entity.Organization;
import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipRole;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import com.wastina.trust.entity.OrganizationStatus;
import com.wastina.trust.entity.Role;
import com.wastina.trust.entity.User;
import com.wastina.trust.entity.WorkerProfile;
import com.wastina.trust.repository.EmployerProfileRepository;
import com.wastina.trust.repository.GuarantorProfileRepository;
import com.wastina.trust.repository.OrganizationMembershipRepository;
import com.wastina.trust.repository.OrganizationRepository;
import com.wastina.trust.repository.RoleRepository;
import com.wastina.trust.repository.UserRepository;
import com.wastina.trust.repository.WorkerProfileRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions the synthetic local accounts that back the {@link DevelopmentPersona}s.
 *
 * <p>Runs only when {@code wastina.dev.jwt.enabled=true}. Each persona gets a stable {@code identitySubject} that
 * the local token issuer writes into the JWT {@code sub} claim, a matching email/phone, and a
 * persisted {@link Role} so that {@code /api/v1/me} reports the same role the token carries.
 *
 * <p>A development organization is also seeded so the employment and employment-verification
 * flows are reachable: the employer persona owns it (and can therefore verify employment), and
 * the worker persona is a member (and can therefore create an employment record against it).
 */
@Component
@ConditionalOnProperty(prefix = "wastina.dev.jwt", name = "enabled", havingValue = "true")
public class DevelopmentAccountSeeder implements ApplicationRunner {

    private static final String ORGANIZATION_NAME = "Wastina Development Organization";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final GuarantorProfileRepository guarantorProfileRepository;

    public DevelopmentAccountSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            WorkerProfileRepository workerProfileRepository,
            EmployerProfileRepository employerProfileRepository,
            GuarantorProfileRepository guarantorProfileRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.workerProfileRepository = workerProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.guarantorProfileRepository = guarantorProfileRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DevelopmentPersona persona : DevelopmentPersona.values()) {
            User user = upsertUser(persona);
            ensureProfile(user, persona);
        }
        seedDevelopmentOrganization();
    }

    private User upsertUser(DevelopmentPersona persona) {
        ERole role = ERole.valueOf(persona.getRole());
        Set<Role> roles = EnumSet.of(role).stream()
                .map(candidate -> roleRepository.findByName(candidate)
                        .orElseGet(() -> roleRepository.save(new Role(candidate))))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        // First, try to find by identitySubject (same persona, same run or previous run)
        Optional<User> existing = userRepository.findByIdentitySubject(persona.getSubject());
        if (existing.isPresent()) {
            User user = existing.get();
            user.setEmail(persona.getEmail());
            user.setPhoneNumber(persona.getPhoneNumber());
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setRoles(roles);
            return userRepository.save(user);
        }

        // Not found by subject — check if email/phone belong to another account
        // and migrate that account to this persona (dev-only convenience).
        Optional<User> byEmail = userRepository.findByEmail(persona.getEmail());
        Optional<User> byPhone = userRepository.findByPhoneNumber(persona.getPhoneNumber());

        User user;
        if (byEmail.isPresent()) {
            user = byEmail.get();
        } else if (byPhone.isPresent()) {
            user = byPhone.get();
        } else {
            user = new User(persona.getPhoneNumber());
        }

        user.setEmail(persona.getEmail());
        user.setPhoneNumber(persona.getPhoneNumber());
        user.setIdentitySubject(persona.getSubject());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    private void ensureProfile(User user, DevelopmentPersona persona) {
        if (persona == DevelopmentPersona.WORKER && !workerProfileRepository.existsById(user.getId())) {
            workerProfileRepository.save(new WorkerProfile(user));
        }
        if (persona == DevelopmentPersona.EMPLOYER && !employerProfileRepository.existsById(user.getId())) {
            EmployerProfile profile = new EmployerProfile(user);
            profile.setCompanyName(ORGANIZATION_NAME);
            employerProfileRepository.save(profile);
        }
        if (persona == DevelopmentPersona.GUARANTOR && !guarantorProfileRepository.existsById(user.getId())) {
            GuarantorProfile profile = new GuarantorProfile(user);
            profile.setEmployerName(ORGANIZATION_NAME);
            guarantorProfileRepository.save(profile);
        }
    }

    private void seedDevelopmentOrganization() {
        Organization organization = organizationRepository.findFirstByName(ORGANIZATION_NAME)
                .orElseGet(() -> {
                    Organization created = new Organization(ORGANIZATION_NAME);
                    created.setLegalName("Wastina Development Organization PLC");
                    created.setStatus(OrganizationStatus.VERIFIED);
                    return organizationRepository.save(created);
                });

        // The employer owns the organization, which grants employment-verification authority.
        // The worker is a plain member, which is enough to create an employment record against it.
        ensureMembership(organization, DevelopmentPersona.EMPLOYER, OrganizationMembershipRole.OWNER);
        ensureMembership(organization, DevelopmentPersona.WORKER, OrganizationMembershipRole.EMPLOYEE);
    }

    private void ensureMembership(Organization organization, DevelopmentPersona persona,
                                  OrganizationMembershipRole role) {
        User user = userRepository.findByIdentitySubject(persona.getSubject()).orElse(null);
        if (user == null) {
            return;
        }
        if (membershipRepository.findByOrganizationAndUser(organization, user).isPresent()) {
            return;
        }
        membershipRepository.save(new OrganizationMembership(
                user, organization, role, OrganizationMembershipStatus.ACTIVE));
    }
}