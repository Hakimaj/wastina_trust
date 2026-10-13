package com.wastina.trust.repository;

import com.wastina.trust.entity.OrganizationInvitation;
import com.wastina.trust.entity.OrganizationInvitationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

    Optional<OrganizationInvitation> findByCodeHash(String codeHash);

    List<OrganizationInvitation> findByOrganization_IdAndStatusOrderByCreatedAtDesc(
            UUID organizationId, OrganizationInvitationStatus status);

    long countByOrganization_IdAndStatus(UUID organizationId, OrganizationInvitationStatus status);
}