package com.wastina.trust.repository;

import com.wastina.trust.entity.OrganizationMembership;
import com.wastina.trust.entity.OrganizationMembershipRole;
import com.wastina.trust.entity.OrganizationMembershipStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    java.util.Optional<OrganizationMembership> findByOrganizationAndUser(
            com.wastina.trust.entity.Organization organization, com.wastina.trust.entity.User user);

    List<OrganizationMembership> findByOrganizationId(UUID organizationId);

    List<OrganizationMembership> findByUserId(UUID userId);

    boolean existsByOrganizationIdAndUserIdAndRole(UUID organizationId, UUID userId,
                                                   OrganizationMembershipRole role);

    boolean existsByOrganizationIdAndUserIdAndStatus(UUID organizationId, UUID userId,
                                                      OrganizationMembershipStatus status);

    Page<OrganizationMembership> findByOrganizationId(UUID organizationId, Pageable pageable);

    long countByOrganization_IdAndStatusAndRoleIn(UUID organizationId,
                                                   OrganizationMembershipStatus status,
                                                   java.util.Collection<OrganizationMembershipRole> roles);
}