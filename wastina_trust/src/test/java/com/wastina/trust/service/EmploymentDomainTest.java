package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmploymentDomainTest {

    @Test
    void employmentRecordRejectsEndDateBeforeStartDate() {
        User worker = new User("+251911000001");
        worker.setId(UUID.randomUUID());

        Organization organization = new Organization("Synthetic Company A");
        organization.setId(UUID.randomUUID());

        EmploymentRecord record = new EmploymentRecord();
        record.setWorker(worker);
        record.setOrganization(organization);
        record.setJobTitleEn("Software Engineer");
        record.setJobTitleAm("የሶፍትዌር መሐንዲስ");
        record.setStartDate(LocalDate.of(2023, 1, 1));
        record.setEndDate(LocalDate.of(2022, 12, 31));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EmploymentRecord.validateDates(record.getStartDate(), record.getEndDate()));

        assertTrue(ex.getMessage().contains("before"));
    }

    @Test
    void organizationMembershipAllowsOnlyAuthorizedVerifierRoles() {
        User admin = new User("+251911000002");
        Organization org = new Organization("Synthetic Company A");

        OrganizationMembership hrManager = new OrganizationMembership(admin, org, OrganizationMembershipRole.HR_MANAGER, OrganizationMembershipStatus.ACTIVE);
        OrganizationMembership employee = new OrganizationMembership(admin, org, OrganizationMembershipRole.EMPLOYEE, OrganizationMembershipStatus.ACTIVE);

        assertTrue(hrManager.canVerifyEmployment());
        assertFalse(employee.canVerifyEmployment());
    }

    @Test
    void verificationRequestProgressesThroughApprovedStateMachine() {
        EmploymentVerificationRequest request = new EmploymentVerificationRequest();

        request.create();
        request.markSent();
        request.markViewed();
        request.markInReview();
        request.markVerified();

        assertEquals(EmploymentVerificationRequestStatus.VERIFIED, request.getStatus());
    }
}
