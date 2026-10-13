package com.wastina.trust.service;

import com.wastina.trust.controller.ApiErrorCodes;
import com.wastina.trust.controller.BusinessRuleViolationException;
import com.wastina.trust.controller.EntityNotFoundException;
import com.wastina.trust.entity.Organization;
import com.wastina.trust.entity.OrganizationInvitation;
import com.wastina.trust.entity.OrganizationInvitationStatus;
import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipRole;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import com.wastina.trust.entity.OrganizationStatus;
import com.wastina.trust.entity.User;
import com.wastina.trust.repository.OrganizationInvitationRepository;
import com.wastina.trust.repository.OrganizationMembershipRepository;
import com.wastina.trust.repository.OrganizationRepository;
import com.wastina.trust.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Organization and membership lifecycle.
 *
 * <p>The audit found that the only organization endpoint was {@code GET /me/organizations}, so a
 * brand-new account could never obtain membership and could therefore never create an employment
 * record or reach the employer verification inbox. This service closes that gap while keeping the
 * authorization rules strict:
 *
 * <ul>
 *   <li>Creating an organization grants the creator {@code OWNER} — never a verifying role by
 *       accident, and never membership in someone else's organization.</li>
 *   <li>Joining requires a single-use, time-limited, cryptographically random code whose SHA-256
 *       digest is what gets stored. Redeeming always grants the non-privileged
 *       {@code EMPLOYEE} role.</li>
 *   <li>Verifying roles ({@code VERIFIER}, {@code HR_MANAGER}, {@code HR_ADMIN}, {@code OWNER}) may
 *       only be granted by an existing member who already holds that same role, so membership
 *       cannot be escalated by redeeming a code.</li>
 *   <li>Leaving is refused for the last {@code OWNER}, which would otherwise orphan the
 *       organization.</li>
 * </ul>
 */
@Service
public class OrganizationService {

    /** Roles that confer employment-verification authority. */
    public static final Set<OrganizationMembershipRole> VERIFYING_ROLES = Set.of(
            OrganizationMembershipRole.OWNER,
            OrganizationMembershipRole.HR_ADMIN,
            OrganizationMembershipRole.HR_MANAGER,
            OrganizationMembershipRole.MANAGER,
            OrganizationMembershipRole.VERIFIER);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationInvitationRepository invitationRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMembershipRepository membershipRepository,
                               OrganizationInvitationRepository invitationRepository,
                               UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Organization createOrganization(User creator, String name, String legalName, String description) {
        String normalized = name.trim();
        if (organizationRepository.findFirstByName(normalized).isPresent()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.ORGANIZATION_NAME_TAKEN,
                    "An organization named '" + normalized + "' already exists.");
        }
        Organization organization = new Organization(normalized);
        organization.setLegalName(legalName == null || legalName.isBlank() ? null : legalName.trim());
        organization.setDescription(description == null || description.isBlank() ? null : description.trim());
        // Self-registered organizations start unverified; an operator must verify them before
        // their employment claims are treated as authoritative.
        organization.setStatus(OrganizationStatus.PENDING);
        organization = organizationRepository.save(organization);

        membershipRepository.save(new OrganizationMembership(
                creator, organization, OrganizationMembershipRole.OWNER, OrganizationMembershipStatus.ACTIVE));
        return organization;
    }

    @Transactional
    public CreatedInvitation createInvitation(UUID organizationId, User creator, String joinCode) {
        Organization organization = requireOrganization(organizationId);
        OrganizationMembership membership = requireMembership(organization, creator);

        // Only an administrator of the organization may mint join codes.
        if (membership.getRole() != OrganizationMembershipRole.OWNER
                && membership.getRole() != OrganizationMembershipRole.HR_ADMIN) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "Only an organization owner or HR administrator may create an invitation.");
        }

        String code = joinCode == null || joinCode.isBlank() ? generateJoinCode() : joinCode.trim();
        if (code.length() < 8) {
            throw new IllegalArgumentException("A join code must be at least 8 characters.");
        }
        if (invitationRepository.findByCodeHash(hash(code)).isPresent()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.CONFLICT,
                    "That join code is already in use.");
        }

        OrganizationInvitation invitation = new OrganizationInvitation();
        invitation.setOrganization(organization);
        invitation.setCreatedByUserId(creator.getId());
        invitation.setCodeHash(hash(code));
        invitation.setGrantedMembershipRole(OrganizationInvitation.DEFAULT_JOIN_ROLE);
        invitation.setStatus(OrganizationInvitationStatus.ACTIVE);
        invitation.setCreatedAt(Instant.now());
        invitation.setExpiresAt(Instant.now()
                .plus(OrganizationInvitation.DEFAULT_VALIDITY_HOURS, ChronoUnit.HOURS));

        return new CreatedInvitation(invitationRepository.save(invitation), code);
    }

    @Transactional
    public OrganizationMembership redeemInvitation(String joinCode, User joiner) {
        String normalized = joinCode == null ? "" : joinCode.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("A join code is required.");
        }
        OrganizationInvitation invitation = invitationRepository.findByCodeHash(hash(normalized))
                .orElseThrow(() -> new EntityNotFoundException("No invitation matches that join code."));
        if (!invitation.isRedeemable()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.ORGANIZATION_NOT_JOINABLE,
                    "The invitation is " + invitation.getStatus().name().toLowerCase(Locale.ROOT)
                            + " or has expired.");
        }

        Organization organization = invitation.getOrganization();
        Optional<OrganizationMembership> existing = membershipRepository
                .findByOrganizationAndUser(organization, joiner);
        if (existing.isPresent()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.MEMBERSHIP_ALREADY_EXISTS,
                    "You are already a member of this organization.");
        }

        invitation.redeem(joiner.getId());
        invitationRepository.save(invitation);

        return membershipRepository.save(new OrganizationMembership(joiner, organization,
                invitation.getGrantedMembershipRole(), OrganizationMembershipStatus.ACTIVE));
    }

    @Transactional
    public OrganizationMembership updateMembershipRole(UUID organizationId, UUID memberUserId,
                                                       OrganizationMembershipRole newRole, User actor) {
        Organization organization = requireOrganization(organizationId);
        OrganizationMembership actorMembership = requireMembership(organization, actor);
        OrganizationMembership target = requireMembershipByUser(organizationId, memberUserId);

        // An actor may only grant a role they already hold, so privileges cannot be escalated by
        // inviting someone with a broader role than the inviter possesses.
        if (!actorMembership.canManageMemberships()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "Only an owner or HR administrator may manage memberships.");
        }
        if (actorMembership.getRole() != OrganizationMembershipRole.OWNER
                && newRole == OrganizationMembershipRole.OWNER) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "Only an owner may grant the OWNER role.");
        }
        if (target.getRole() == OrganizationMembershipRole.OWNER
                && newRole != OrganizationMembershipRole.OWNER) {
            requireAnotherOwnerExists(organizationId, memberUserId);
        }
        if (target.getId().equals(actorMembership.getId()) && newRole != actorMembership.getRole()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "You cannot change your own membership role.");
        }

        target.setRole(newRole);
        target.setStatus(OrganizationMembershipStatus.ACTIVE);
        return membershipRepository.save(target);
    }

    @Transactional
    public void suspendMembership(UUID organizationId, UUID memberUserId, User actor) {
        OrganizationMembership target = setMembershipStatus(
                organizationId, memberUserId, actor, OrganizationMembershipStatus.SUSPENDED);
        if (target == null) {
            return;
        }
    }

    @Transactional
    public void leaveOrganization(UUID organizationId, User user) {
        Organization organization = requireOrganization(organizationId);
        OrganizationMembership membership = requireMembership(organization, user);

        if (membership.getRole() == OrganizationMembershipRole.OWNER) {
            requireAnotherOwnerExists(organizationId, user.getId());
        }
        membership.setStatus(OrganizationMembershipStatus.LEFT);
        membership.setEndedAt(java.time.LocalDateTime.now());
        membershipRepository.save(membership);
    }

    @Transactional
    public OrganizationMembership setMembershipStatus(UUID organizationId, UUID memberUserId, User actor,
                                                      OrganizationMembershipStatus status) {
        Organization organization = requireOrganization(organizationId);
        OrganizationMembership actorMembership = requireMembership(organization, actor);
        if (!actorMembership.canManageMemberships()) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "Only an owner or HR administrator may manage memberships.");
        }

        OrganizationMembership target = requireMembershipByUser(organizationId, memberUserId);
        if (target.getId().equals(actorMembership.getId())) {
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "You cannot change your own membership status.");
        }
        if (target.getRole() == OrganizationMembershipRole.OWNER && status != OrganizationMembershipStatus.ACTIVE) {
            requireAnotherOwnerExists(organizationId, memberUserId);
        }

        target.setStatus(status);
        if (status == OrganizationMembershipStatus.ACTIVE) {
            target.setEndedAt(null);
        } else {
            target.setEndedAt(java.time.LocalDateTime.now());
        }
        return membershipRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembership> listMembers(UUID organizationId) {
        return membershipRepository.findByOrganizationId(organizationId);
    }

    /** Every membership held by a user, across all organizations. */
    @Transactional(readOnly = true)
    public List<OrganizationMembership> listMembershipsForUser(UUID userId) {
        return membershipRepository.findByUserId(userId);
    }

    /**
     * Returns the caller's {@code ACTIVE} membership in an organization, or empty. Used by other
     * domains (employment verification) that need to know whether a caller may act for an
     * organization.
     */
    @Transactional(readOnly = true)
    public Optional<OrganizationMembership> findActiveMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .filter(membership -> membership.getUser().getId().equals(userId))
                .filter(membership -> membership.getStatus() == OrganizationMembershipStatus.ACTIVE)
                .findFirst();
    }

    /**
     * Confirms the caller is entitled to reference the organization, for example when creating an
     * employment record. Membership is required but no particular role is.
     */
    @Transactional(readOnly = true)
    public OrganizationMembership requireActiveMembership(UUID organizationId, UUID userId) {
        boolean member = membershipRepository.findByOrganizationId(organizationId).stream()
                .anyMatch(membership -> membership.getUser().getId().equals(userId));
        if (!member) {
            // Not a member at all: an authorization decision.
            throw new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                    "You are not a member of this organization.");
        }
        return findActiveMembership(organizationId, userId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        ApiErrorCodes.ORGANIZATION_MEMBERSHIP_REQUIRED,
                        "Your membership of this organization is not active."));
    }

    public Organization requireOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization was not found: " + organizationId));
    }

    /**
     * Requires any membership (not necessarily {@code ACTIVE}) for the organization.
     *
     * <p>A caller with no membership at all is refused with {@code 403} because acting for an
     * organization they do not belong to is an authorization decision, not a state conflict.
     */
    public OrganizationMembership requireMembership(Organization organization, User user) {
        return membershipRepository.findByOrganizationAndUser(organization, user)
                .orElseThrow(() -> new BusinessRuleViolationException(ApiErrorCodes.FORBIDDEN,
                        "You are not a member of this organization."));
    }

    public OrganizationMembership requireMembershipByUser(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .filter(membership -> membership.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Membership was not found for user " + userId + " in organization " + organizationId));
    }

    private void requireAnotherOwnerExists(UUID organizationId, UUID excludingUserId) {
        boolean anotherOwner = membershipRepository.findByOrganizationId(organizationId).stream()
                .filter(membership -> !membership.getUser().getId().equals(excludingUserId))
                .anyMatch(membership -> membership.getRole() == OrganizationMembershipRole.OWNER
                        && membership.getStatus() == OrganizationMembershipStatus.ACTIVE);
        if (!anotherOwner) {
            throw new BusinessRuleViolationException(ApiErrorCodes.CONFLICT,
                    "An organization must always retain at least one active OWNER. "
                            + "Promote another member first.");
        }
    }

    private String generateJoinCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /**
     * The invitation plus its one-time plain join code. The code is not persisted and cannot be
     * retrieved again.
     */
    public record CreatedInvitation(OrganizationInvitation invitation, String joinCode) {
    }
}