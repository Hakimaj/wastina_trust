package com.wastina.trust.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wastina.trust.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the compliance authorization findings.
 *
 * <p>The audited original trusted a body-supplied {@code ComplianceRole} and a
 * {@code caseScopeGranted} boolean as its authorization decision, so any authenticated user could
 * open, decide and escalate cases. Actor identity is now derived from the JWT and read endpoints
 * require an administrative authority.
 */
class ComplianceAuthorizationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("a complaint is filed as the authenticated caller, not as a body-supplied complainant")
    void complainantIsDerivedFromJwt() throws Exception {
        String workerId = currentUserId(workerToken);

        // Attempting to file as somebody else still files as the caller.
        JsonNode complaint = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"),
                        workerToken)
                        .contentType("application/json")
                        .content("""
                                {"complainantId":"%s","subjectId":"%s","category":"PAYMENT_DEFAULT",
                                 "description":"Employer has not paid the agreed salary for three months."}
                                """.formatted(currentUserId(employerToken), currentUserId(guarantorToken))))
                .andExpect(status().isCreated())
                .andReturn());

        assertTrue(workerId.equals(complaint.get("complainantId").asText()),
                "the complainant must come from the token, got " + complaint.get("complainantId"));
        assertTrue(complaint.get("publicReference").asText().startsWith("WST-CMP-"));
    }

    @Test
    @DisplayName("a complaint response no longer exposes party email addresses or phone numbers")
    void complaintResponseOmitsPersonalData() throws Exception {
        String body = mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"FRAUD",
                                 "description":"This is a sufficiently detailed complaint description."}
                                """.formatted(currentUserId(employerToken))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("@wastina.local"), "party email leaked in: " + body);
        assertFalse(body.contains("+2519"), "party phone leaked in: " + body);
        assertFalse(body.contains("\"roles\""), "nested user roles leaked in: " + body);
    }

    @Test
    @DisplayName("a non-administrator cannot open, investigate or decide a case")
    void nonAdministratorCannotOperateCases() throws Exception {
        JsonNode complaint = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"),
                        workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"ABSENTEEISM",
                                 "description":"Worker has been absent from contracted shifts repeatedly."}
                                """.formatted(currentUserId(employerToken))))
                .andExpect(status().isCreated()).andReturn());
        assertTrue(complaint.get("id").asText() != null);

        // Opening a case requires ROLE_ADMIN.
        mockMvc.perform(withBearer(post("/api/v1/compliance/complaints/"
                        + complaint.get("id").asText() + "/cases"), workerToken)
                        .contentType("application/json")
                        .content("{\"subjectId\":\"" + currentUserId(employerToken) + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("case listing requires an administrator")
    void caseListingRequiresAdmin() throws Exception {
        mockMvc.perform(withBearer(get("/api/v1/compliance/cases"), workerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withBearer(get("/api/v1/compliance/complaints"), workerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withBearer(get("/api/v1/compliance/cases"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("a decision cannot be made by claiming a compliance role in the request body")
    void decisionRoleCannotBeClaimed() throws Exception {
        JsonNode complaint = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"),
                        workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"FALSE_INFORMATION",
                                 "description":"Employment details supplied to the lender were inaccurate."}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated()).andReturn());

        // An administrator opens a case.
        JsonNode caseView = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints/"
                        + complaint.get("id").asText() + "/cases"), adminToken)
                        .contentType("application/json")
                        .content("{\"subjectId\":\"" + currentUserId(guarantorToken) + "\"}"))
                .andExpect(status().isCreated()).andReturn());
        assertTrue(caseView.get("publicReference").asText().startsWith("WST-CASE-"));

        String caseId = caseView.get("id").asText();

        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/investigation"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATION"));

        // A non-administrator cannot decide, even while claiming ADMIN and caseScopeGranted=true.
        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/decision"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"role":"ADMIN","caseScopeGranted":true,
                                 "decisionType":"UPHELD","reason":"Claim upheld."}
                                """))
                .andExpect(status().isForbidden());

        // The administrator decides without needing to assert a role.
        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/decision"), adminToken)
                        .contentType("application/json")
                        .content("{\"decisionType\":\"UPHELD\",\"reason\":\"Claim upheld.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECIDED"))
                .andExpect(jsonPath("$.decisionType").value("UPHELD"));
    }

    @Test
    @DisplayName("an appeal can only be filed by a party to the case")
    void appealRequiresPartyStatus() throws Exception {
        JsonNode complaint = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"),
                        workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"GUARANTEE_BREACH",
                                 "description":"The guarantee terms were not honoured as agreed."}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated()).andReturn());

        JsonNode caseView = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints/"
                        + complaint.get("id").asText() + "/cases"), adminToken)
                        .contentType("application/json")
                        .content("{\"subjectId\":\"" + currentUserId(guarantorToken) + "\"}"))
                .andExpect(status().isCreated()).andReturn());
        String caseId = lookupCaseIdByReference(caseView.get("publicReference").asText());

        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/investigation"), adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/decision"), adminToken)
                        .contentType("application/json")
                        .content("{\"decisionType\":\"UPHELD\",\"reason\":\"Upheld.\"}"))
                .andExpect(status().isOk());

        // An unrelated account cannot appeal.
        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/appeals"), employerToken)
                        .contentType("application/json")
                        .content("{\"grounds\":\"PROCESS_ERROR\"}"))
                .andExpect(status().isForbidden());

        // The complainant who raised it may.
        mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId + "/appeals"), workerToken)
                        .contentType("application/json")
                        .content("{\"grounds\":\"PROCESS_ERROR\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("a compliance action records the authenticated caller as the actor")
    void complianceActionUsesCallerAsActor() throws Exception {
        JsonNode complaint = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"),
                        workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"THEFT",
                                 "description":"Company equipment was taken without authorisation."}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated()).andReturn());

        JsonNode caseView = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/complaints/"
                        + complaint.get("id").asText() + "/cases"), adminToken)
                        .contentType("application/json")
                        .content("{\"subjectId\":\"" + currentUserId(guarantorToken) + "\"}"))
                .andExpect(status().isCreated()).andReturn());
        String caseId = lookupCaseIdByReference(caseView.get("publicReference").asText());

        JsonNode action = readJson(mockMvc.perform(withBearer(post("/api/v1/compliance/cases/" + caseId
                        + "/actions"), adminToken)
                        .contentType("application/json")
                        .content("""
                                {"actorId":"%s","type":"DOCUMENT_ACCESS_RESTRICTION",
                                 "reason":"Evidence integrity is in question.",
                                 "expiresAt":"2030-01-01T00:00:00Z"}
                                """.formatted(currentUserId(workerToken)))
                        )
                .andExpect(status().isCreated()).andReturn());

        assertTrue(currentUserId(adminToken).equals(action.get("appliedBy").asText()),
                "the actor must come from the token, got " + action.get("appliedBy"));
    }

    @Test
    @DisplayName("a complaint description shorter than 20 characters is refused")
    void complaintDescriptionIsValidated() throws Exception {
        mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"OTHER","description":"too short"}
                                """.formatted(currentUserId(employerToken))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a complaint against oneself is refused")
    void selfComplaintIsRefused() throws Exception {
        String workerId = currentUserId(workerToken);

        mockMvc.perform(withBearer(post("/api/v1/compliance/complaints"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"subjectId":"%s","category":"OTHER",
                                 "description":"A sufficiently long description of a complaint."}
                                """.formatted(workerId)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Cases are addressed by id, but every caller only holds the public reference, so this resolves
     * the reference against the administrator case list.
     */
    private String lookupCaseIdByReference(String publicReference) throws Exception {
        JsonNode content = readJson(mockMvc.perform(
                withBearer(get("/api/v1/compliance/cases?size=200"), adminToken)).andReturn())
                .get("content");
        for (JsonNode row : content) {
            if (publicReference.equals(row.get("publicReference").asText())) {
                return row.get("id").asText();
            }
        }
        throw new IllegalStateException("case not found: " + publicReference);
    }

    private String currentUserId(String token) throws Exception {
        return readJson(mockMvc.perform(withBearer(get("/api/v1/me"), token)).andReturn())
                .get("id").asText();
    }
}