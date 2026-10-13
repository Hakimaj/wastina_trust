package com.wastina.trust.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A time-limited invitation that lets a person join an organization without the invitation being
 * guessable.
 *
 * <p>The join code is stored as a SHA-256 digest, never in plain text, so a database disclosure
 * does not hand out join access. The plain code is shown to its creator exactly once, at creation.
 *
 * <p>An invitation grants only {@link #DEFAULT_JOIN_ROLE} by default. Membership roles that carry
 * employment-verification authority must be assigned deliberately by an existing administrator,
 * never by redeeming a code.
 */
@Entity
@Table(name = "organization_invitations",
        uniqueConstraints = @UniqueConstraint(name = "uk_org_invitation_code", columnNames = "code_hash"))
@Data
@NoArgsConstructor
public class OrganizationInvitation {

    /** How long an invitation remains usable, in hours. */
    public static final int DEFAULT_VALIDITY_HOURS = 72;

    /** Role granted to someone who redeems only the join code. Never a verifying role. */
    public static final OrganizationMembershipRole DEFAULT_JOIN_ROLE = OrganizationMembershipRole.EMPLOYEE;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    /** SHA-256 hex digest of the join code. The plain code is never stored. */
    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_membership_role", nullable = false, length = 30)
    private OrganizationMembershipRole grantedMembershipRole = DEFAULT_JOIN_ROLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationInvitationStatus status = OrganizationInvitationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "redeemed_by_user_id")
    private UUID redeemedByUserId;

    @Version
    private Long version;

    public boolean isRedeemable() {
        return status == OrganizationInvitationStatus.ACTIVE
                && expiresAt != null
                && Instant.now().isBefore(expiresAt);
    }

    public void redeem(UUID userId) {
        if (!isRedeemable()) {
            throw new IllegalStateException("The invitation is no longer redeemable.");
        }
        this.status = OrganizationInvitationStatus.REDEEMED;
        this.redeemedAt = Instant.now();
        this.redeemedByUserId = userId;
    }

    public void revoke() {
        if (status == OrganizationInvitationStatus.REDEEMED) {
            throw new IllegalStateException("A redeemed invitation cannot be revoked.");
        }
        this.status = OrganizationInvitationStatus.REVOKED;
    }
}