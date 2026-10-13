package com.wastina.trust.dto;

import com.wastina.trust.entity.EmploymentType;
import com.wastina.trust.entity.SectorType;
import com.wastina.trust.entity.WorkRelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record EmploymentCreateRequestDto(
        @NotNull UUID organizationId,
        @NotBlank String jobTitleEn,
        String jobTitleAm,
        String occupationCode,
        String department,
        String location,
        @NotNull EmploymentType employmentType,
        @NotNull WorkRelationshipType workRelationshipType,
        @NotNull SectorType sector,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        boolean currentEmployment,
        String description) {
}