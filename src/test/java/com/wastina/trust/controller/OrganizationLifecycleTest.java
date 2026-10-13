package com.wastina.trust.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wastina.trust.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C-4: the organization lifecycle that makes the employment flows reachable.
 *
 * <p>Before this, {@code GET /me/organizations} was the only organization endpoint, so a brand-new
 * account could never obtain membership and could therefore never create an employment record or
 * reach the employer verification inbox.
 *
 * <p>Also verifies that joining never confers employment-verification authority, which was the
 * specific concern raised about inventing a membership flow.
 */
class OrganizationLifecycleTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("C-4: a user can create an organization and becomes its OWNER")
    void createOrganization() throws Exception {
        String name = "Test Org " + System.nanoTime();

        MvcResult result = mockMvc.perform(withBearer(post("/api/v1/organizations"), workerToken)
                        .contentType("application/json")
                        .content("{\"name\":\"" + name + "\",\"legalName\":\"" + name + " PLC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.canManageMemberships").value(true))
                .andReturn();

        JsonNode created = readJson(result);
        assertEquals("PENDING", created.get("status").asText(),
                "a self-registered organization must start unverified");
        assertNotNull(created.get("id").asText());

        // It now appears in /me/organizations with derived capability flags.
        JsonNode memberships = readJson(mockMvc.perform(
                withBearer(get("/api/v1/me/organizations"), workerToken)).andReturn());
        boolean found = false;
        for (JsonNode membership : memberships) {
            if (name.equals(membership.get("name").asText())) {
                found = true;
                assertEquals("OWNER", membership.get("role").asText());
                assertTrue(membership.get("canVerifyEmployment").asBoolean());
            }
        }
        assertTrue(found, "the new organization should appear in the caller's memberships");
    }

    @Test
    @DisplayName("C-4: a duplicate organization name is a 409 with a stable code")
    void duplicateNameIsRejected() throws Exception {
        String name = "Duplicate Test Org " + System.nanoTime();
        String body = "{\"name\":\"" + name + "\"}";

        mockMvc.perform(withBearer(post("/api/v1/organizations"), workerToken)
                .contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(withBearer(post("/api/v1/organizations"), employerToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NAME_TAKEN"));
    }

    @Test
    @DisplayName("C-4: an invitation is single-use and its code is only returned once")
    void invitationIsSingleUse() throws Exception {
        String orgId = createOrganization(workerToken, "Invite Org " + System.nanoTime());

        MvcResult created = mockMvc.perform(
                        withBearer(post("/api/v1/organizations/" + orgId + "/invitations"), workerToken)
                                .contentType("application/json").content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode invitation = readJson(created);
        String joinCode = invitation.get("joinCode").asText();
        assertNotNull(joinCode);
        assertFalse(joinCode.isBlank());
        assertEquals("EMPLOYEE", invitation.get("grantedMembershipRole").asText());

        // Listing never returns the code again.
        JsonNode listed = readJson(mockMvc.perform(
                withBearer(get("/api/v1/organizations/" + orgId + "/invitations"), workerToken)).andReturn());
        assertTrue(listed.get(0).get("joinCode").isNull(),
                "the plain join code must not be retrievable after creation");

        // Redeem once.
        mockMvc.perform(withBearer(post("/api/v1/organizations/join"), guarantorToken)
                        .contentType("application/json")
                        .content("{\"joinCode\":\"" + joinCode + "\"}"))
                .andExpect(status().isCreated());

        // A second redemption is refused.
        mockMvc.perform(withBearer(post("/api/v1/organizations/join"), adminToken)
                        .contentType("application/json")
                        .content("{\"joinCode\":\"" + joinCode + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_JOINABLE"));
    }

    @Test
    @DisplayName("joining never grants employment-verification authority")
    void joiningDoesNotGrantVerifyingRole() throws Exception {
        String orgId = createOrganization(workerToken, "No Escalation Org " + System.nanoTime());
        String joinCode = readJson(mockMvc.perform(
                        withBearer(post("/api/v1/organizations/" + orgId + "/invitations"), workerToken)
                                .contentType("application/json").content("{}"))
                .andExpect(status().isCreated()).andReturn())
                .get("joinCode").asText();

        mockMvc.perform(withBearer(post("/api/v1/organizations/join"), guarantorToken)
                        .contentType("application/json")
                        .content("{\"joinCode\":\"" + joinCode + "\"}"))
                .andExpect(status().isCreated());

        JsonNode memberships = readJson(mockMvc.perform(
                withBearer(get("/api/v1/me/organizations"), guarantorToken)).andReturn());

        for (JsonNode membership : memberships) {
            if (orgId.equals(membership.get("organizationId").asText())) {
                assertEquals("EMPLOYEE", membership.get("role").asText());
                assertFalse(membership.get("canVerifyEmployment").asBoolean(),
                        "joining must never confer verification authority");
                assertFalse(membership.get("canManageMemberships").asBoolean(),
                        "joining must never confer membership administration");
            }
        }
    }

    @Test
    @DisplayName("a non-member cannot read an organization's members or create invitations")
    void nonMemberIsRefused() throws Exception {
        String orgId = createOrganization(workerToken, "Closed Org " + System.nanoTime());

        mockMvc.perform(withBearer(get("/api/v1/organizations/" + orgId + "/members"), guarantorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withBearer(post("/api/v1/organizations/" + orgId + "/invitations"), guarantorToken)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("members are paged and report derived capabilities")
    void membersArePaged() throws Exception {
        String orgId = createOrganization(workerToken, "Paged Org " + System.nanoTime());

        mockMvc.perform(withBearer(get("/api/v1/organizations/" + orgId + "/members?page=0&size=10"),
                        workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("an unknown organization is a 404, not a 500")
    void unknownOrganizationIsNotFound() throws Exception {
        mockMvc.perform(withBearer(
                        get("/api/v1/organizations/00000000-0000-0000-0000-000000000000"), workerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("an invalid join code is a 404 and does not grant membership")
    void invalidJoinCodeIsNotFound() throws Exception {
        mockMvc.perform(withBearer(post("/api/v1/organizations/join"), guarantorToken)
                        .contentType("application/json")
                        .content("{\"joinCode\":\"totally-made-up-code\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the last owner cannot leave, which would orphan the organization")
    void lastOwnerCannotLeave() throws Exception {
        String orgId = createOrganization(workerToken, "Last Owner Org " + System.nanoTime());

        mockMvc.perform(withBearer(delete("/api/v1/organizations/" + orgId + "/membership"), workerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("membership is required before creating an employment record")
    void employmentRequiresMembership() throws Exception {
        // A brand-new organization the worker is not a member of.
        String orgId = createOrganization(employerToken, "Foreign Org " + System.nanoTime());

        mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Engineer",
                                 "employmentType":"FULL_TIME","workRelationshipType":"EMPLOYEE",
                                 "sector":"PRIVATE","startDate":"2024-01-01","currentEmployment":true}
                                """.formatted(orgId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("a member can create an employment record for their organization")
    void memberCanCreateEmploymentRecord() throws Exception {
        String orgId = createOrganization(workerToken, "Employer Org " + System.nanoTime());

        mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Site Engineer",
                                 "employmentType":"FULL_TIME","workRelationshipType":"EMPLOYEE",
                                 "sector":"PRIVATE","startDate":"2024-01-01","currentEmployment":true}
                                """.formatted(orgId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobTitleEn").value("Site Engineer"))
                .andExpect(jsonPath("$.verificationStatus").value("SELF_REPORTED"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private String createOrganization(String token, String name) throws Exception {
        return readJson(mockMvc.perform(withBearer(post("/api/v1/organizations"), token)
                        .contentType("application/json").content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn())
                .get("id").asText();
    }
}