package com.wastina.trust.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "user_id"})
}, indexes = {
        @Index(name = "idx_org_membership_user", columnList = "user_id"),
        @Index(name = "idx_org_membership_org", columnList = "organization_id")
})
@Data
@NoArgsConstructor
public class OrganizationMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationMembershipRole role = OrganizationMembershipRole.EMPLOYEE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationMembershipStatus status = OrganizationMembershipStatus.ACTIVE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public OrganizationMembership(User user, Organization organization, OrganizationMembershipRole role, OrganizationMembershipStatus status) {
        this.user = user;
        this.organization = organization;
        this.role = role;
        this.status = status;
        this.joinedAt = LocalDateTime.now();
    }

    public boolean canVerifyEmployment() {
        return status == OrganizationMembershipStatus.ACTIVE
                && (role == OrganizationMembershipRole.OWNER
                || role == OrganizationMembershipRole.HR_ADMIN
                || role == OrganizationMembershipRole.HR_MANAGER
                || role == OrganizationMembershipRole.MANAGER
                || role == OrganizationMembershipRole.VERIFIER);
    }
}
