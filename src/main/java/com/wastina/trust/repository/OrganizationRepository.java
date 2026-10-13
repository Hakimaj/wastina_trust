package com.wastina.trust.repository;

import com.wastina.trust.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
	Optional<Organization> findFirstByName(String name);
}
