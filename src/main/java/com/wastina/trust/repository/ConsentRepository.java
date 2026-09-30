package com.wastina.trust.repository;

import com.wastina.trust.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {
}