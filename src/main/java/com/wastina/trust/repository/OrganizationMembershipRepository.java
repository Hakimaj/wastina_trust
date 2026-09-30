package com.wastina.trust.repository;

import com.wastina.trust.entity.Organization;
import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipRole;
import com.wastina.trust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    Optional<OrganizationMembership> findByOrganizationAndUser(Organization organization, User user);
    List<OrganizationMembership> findByOrganizationId(UUID organizationId);
    List<OrganizationMembership> findByUserId(UUID userId);
    boolean existsByOrganizationIdAndUserIdAndRole(UUID organizationId, UUID userId, OrganizationMembershipRole role);
}
