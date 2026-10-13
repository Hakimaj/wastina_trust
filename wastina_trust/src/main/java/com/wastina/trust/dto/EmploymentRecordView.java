package com.wastina.trust.dto;

import com.wastina.trust.entity.EmploymentRecord;

import java.time.LocalDate;
import java.util.UUID;

public record EmploymentRecordView(
        UUID id,
        UUID organizationId,
        String organizationName,
        String jobTitleEn,
        String jobTitleAm,
        String occupationCode,
        String department,
        String location,
        String employmentType,
        String workRelationshipType,
        String sector,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentEmployment,
        String description,
        String status,
        String verificationStatus) {
    public static EmploymentRecordView from(EmploymentRecord record) {
        return new EmploymentRecordView(
                record.getId(),
                record.getOrganization().getId(),
                record.getOrganization().getName(),
                record.getJobTitleEn(),
                record.getJobTitleAm(),
                record.getOccupationCode(),
                record.getDepartment(),
                record.getLocation(),
                record.getEmploymentType().name(),
                record.getWorkRelationshipType().name(),
                record.getSector().name(),
                record.getStartDate(),
                record.getEndDate(),
                record.isCurrentEmployment(),
                record.getDescription(),
                record.getStatus().name(),
                record.getVerificationStatus().name());
    }
}