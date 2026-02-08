package com.wastina.trust.repository;

import com.wastina.trust.entity.GuarantorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GuarantorProfileRepository extends JpaRepository<GuarantorProfile, UUID> {
}
