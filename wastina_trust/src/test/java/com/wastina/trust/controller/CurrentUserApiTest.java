package com.wastina.trust.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wastina.trust.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers audit findings C-2 (role assignment) and C-3 (current-user representation), and the
 * requirement that a caller cannot escalate their own privileges.
 */
class CurrentUserApiTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("C-3: /me now returns email and phoneNumber alongside the legacy keys")
    void currentUserIncludesContactDetails() throws Exception {
        JsonNode me = readJson(mockMvc.perform(withBearer(get("/api/v1/me"), workerToken)).andReturn());

        assertEquals("worker@wastina.local", me.get("email").asText());
        assertEquals("+251900000001", me.get("phoneNumber").asText());

        // Legacy keys preserved so an existing frontend keeps working.
        assertEquals("ACTIVE", me.get("account_status").asText());
        assertNotNull(me.get("identity_status"));
        assertTrue(me.get("roles").isArray());
        assertNotNull(me.get("id"));
    }

    @Test
    @DisplayName("C-3: PATCH /me returns the updated representation")
    void patchReturnsUpdatedRepresentation() throws Exception {
        MvcResult result = mockMvc.perform(withBearer(patch("/api/v1/me"), adminToken)
                        .contentType("application/json")
                        .content("{\"email\":\"admin.updated@wastina.local\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readJson(result);
        assertEquals("admin.updated@wastina.local", body.get("email").asText());

        // And the change is durable: a subsequent read agrees.
        JsonNode reread = readJson(mockMvc.perform(withBearer(get("/api/v1/me"), adminToken)).andReturn());
        assertEquals("admin.updated@wastina.local", reread.get("email").asText());
    }

    @Test
    @DisplayName("C-2: a provisioned user receives persisted roles")
    void provisionPersistsRoles() throws Exception {
        JsonNode me = readJson(mockMvc.perform(withBearer(post("/api/v1/me/provision"), workerToken)
                        .contentType("application/json")
                        .content("{\"phoneNumber\":\"+251900000001\"}"))
                .andExpect(status().isOk())
                .andReturn());

        assertTrue(me.get("roles").toString().contains("ROLE_WORKER"),
                "expected ROLE_WORKER in " + me.get("roles"));
        assertEquals("ACTIVE", me.get("account_status").asText());
    }

    @Test
    @DisplayName("C-2: a caller cannot grant themselves ROLE_ADMIN by asking for it")
    void provisionCannotSelfEscalateToAdmin() throws Exception {
        MvcResult result = mockMvc.perform(withBearer(post("/api/v1/me/provision"), workerToken)
                        .contentType("application/json")
                        .content("{\"phoneNumber\":\"+251900000001\",\"roles\":[\"ROLE_ADMIN\"]}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roles = readJson(result).get("roles");
        assertTrue(!roles.toString().contains("ROLE_ADMIN"),
                "self-service provisioning must not grant ROLE_ADMIN, got " + roles);
        assertTrue(roles.toString().contains("ROLE_WORKER"));
    }

    @Test
    @DisplayName("C-3: an invalid email is a 400 with a machine-readable code, not a 500")
    void invalidEmailIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(withBearer(patch("/api/v1/me"), adminToken)
                        .contentType("application/json")
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("provisioning without a phone number is a 400")
    void provisionRequiresPhoneNumber() throws Exception {
        mockMvc.perform(withBearer(post("/api/v1/me/provision"), workerToken)
                        .contentType("application/json")
                        .content("{\"email\":\"worker@wastina.local\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("error responses use stable codes and never leak a stack trace")
    void errorEnvelopeIsStable() throws Exception {
        mockMvc.perform(withBearer(get("/api/v1/me/nope"), workerToken))
                .andExpect(status().isNotFound());

        MvcResult malformed = mockMvc.perform(withBearer(post("/api/v1/me/provision"), workerToken)
                        .contentType("application/json")
                        .content("{ this is not json "))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode error = readJson(malformed);
        assertEquals("MALFORMED_REQUEST", error.get("code").asText());
        assertNotNull(error.get("timestamp").asText());
        assertEquals(-1, error.toString().indexOf("at com.wastina"),
                "response must not contain a stack trace");
    }

    @Test
    @DisplayName("an unknown enum value in the body is a 400, not a 500")
    void unknownEnumValueIsRejected() throws Exception {
        mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Engineer",
                                 "employmentType":"NOT_A_REAL_TYPE","workRelationshipType":"EMPLOYEE",
                                 "sector":"PRIVATE","startDate":"2024-01-01","currentEmployment":true}
                                """.formatted(currentUserId(workerToken))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("C-5: kycStatus submitted by a client is ignored, not stored")
    void kycStatusIsNotClientWritable() throws Exception {
        String guarantorId = currentUserId(guarantorToken);

        mockMvc.perform(withBearer(put("/api/guarantors/" + guarantorId + "/profile"), guarantorToken)
                        .contentType("application/json")
                        .content("{\"financialLimit\":500,\"employerName\":\"Ministry\",\"kycStatus\":\"VERIFIED\"}"))
                .andExpect(status().isOk());

        JsonNode profile = readJson(mockMvc.perform(withBearer(
                        get("/api/guarantors/" + guarantorId + "/profile"), guarantorToken)).andReturn());

        assertTrue(profile.get("kycStatus") == null || profile.get("kycStatus").isNull(),
                "a client must not be able to self-declare KYC status; got " + profile.get("kycStatus"));
    }

    @Test
    @DisplayName("profile validation rejects a blank required field")
    void profileValidationRejectsBlankFields() throws Exception {
        String employerId = currentUserId(employerToken);

        mockMvc.perform(withBearer(put("/api/employers/" + employerId + "/profile"), employerToken)
                        .contentType("application/json")
                        .content("{\"companyName\":\"\",\"locationName\":\"Addis Ababa\",\"subscriptionTier\":\"FREE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(withBearer(put("/api/employers/" + employerId + "/profile"), employerToken)
                        .contentType("application/json")
                        .content("{\"companyName\":\"Acme\",\"locationName\":\"Addis Ababa\",\"subscriptionTier\":\"GOLD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String currentUserId(String token) throws Exception {
        return readJson(mockMvc.perform(withBearer(get("/api/v1/me"), token)).andReturn())
                .get("id").asText();
    }
}