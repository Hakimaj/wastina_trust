package com.wastina.trust.repository;

import com.wastina.trust.entity.EmploymentRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmploymentRatingRepository extends JpaRepository<EmploymentRating, UUID> {
}
