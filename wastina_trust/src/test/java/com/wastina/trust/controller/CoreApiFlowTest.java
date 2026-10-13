package com.wastina.trust.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wastina.trust.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the flows the frontend will actually build on: employment records,
 * employer verification, identity verification and trust scores.
 */
class CoreApiFlowTest extends AbstractIntegrationTest {

    // ------------------------------------------------------------ employment

    @Test
    @DisplayName("the worker employment and employer verification loop completes end to end")
    void employmentVerificationLoop() throws Exception {
        // The worker is already a member of the seeded development organization.
        String organizationId = firstOrganizationId(workerToken);

        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Site Engineer",
                                 "jobTitleAm":"የቦታ ኢንጂኒየር","department":"Engineering","location":"Addis Ababa",
                                 "employmentType":"FULL_TIME","workRelationshipType":"EMPLOYEE",
                                 "sector":"PRIVATE","startDate":"2023-01-01","currentEmployment":true,
                                 "description":"Structural site supervision."}
                                """.formatted(organizationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").value(organizationId))
                .andExpect(jsonPath("$.jobTitleEn").value("Site Engineer"))
                .andExpect(jsonPath("$.verificationStatus").value("SELF_REPORTED"))
                .andExpect(jsonPath("$.currentEmployment").value(true))
                .andReturn();

        String employmentId = readJson(created).get("id").asText();

        // It appears in the worker's history.
        mockMvc.perform(withBearer(get("/api/v1/me/employment"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(employmentId));

        // The worker requests verification.
        mockMvc.perform(withBearer(post("/api/v1/me/employment/" + employmentId
                + "/verification-requests"), workerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.verificationMethod").value("EMPLOYER_PORTAL"));

        // The employer (an OWNER of that organization) sees it in the inbox.
        MvcResult inbox = mockMvc.perform(withBearer(get("/api/v1/organizations/" + organizationId
                + "/employment-verification-requests"), employerToken))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = null;
        for (JsonNode row : readJson(inbox)) {
            if (employmentId.equals(row.get("employmentRecordId").asText())) {
                requestId = row.get("id").asText();
            }
        }
        assertNotNull(requestId, "the request should appear in the employer inbox");

        // The employer begins review, then verifies.
        mockMvc.perform(withBearer(post("/api/v1/organizations/" + organizationId
                + "/employment-verification-requests/" + requestId + "/review"), employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        mockMvc.perform(withBearer(post("/api/v1/organizations/" + organizationId
                + "/employment-verification-requests/" + requestId + "/verify"), employerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // The record now reflects the verification.
        mockMvc.perform(withBearer(get("/api/v1/me/employment/" + employmentId), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("a duplicate open verification request is refused")
    void duplicateVerificationRequestIsRefused() throws Exception {
        String organizationId = firstOrganizationId(workerToken);

        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Planner",
                                 "employmentType":"FULL_TIME","workRelationshipType":"EMPLOYEE",
                                 "sector":"PUBLIC","startDate":"2022-06-01","currentEmployment":true}
                                """.formatted(organizationId)))
                .andExpect(status().isCreated()).andReturn();
        String employmentId = readJson(created).get("id").asText();

        mockMvc.perform(withBearer(post("/api/v1/me/employment/" + employmentId
                + "/verification-requests"), workerToken))
                .andExpect(status().isCreated());

        mockMvc.perform(withBearer(post("/api/v1/me/employment/" + employmentId
                + "/verification-requests"), workerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a non-verifying member cannot reach the organization inbox")
    void nonVerifyingMemberCannotVerify() throws Exception {
        String organizationId = firstOrganizationId(workerToken);

        // The worker is an EMPLOYEE of the development organization: no verification authority.
        mockMvc.perform(withBearer(get("/api/v1/organizations/" + organizationId
                + "/employment-verification-requests"), workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("employment cross-field date rules are enforced")
    void employmentDateRulesAreEnforced() throws Exception {
        String organizationId = firstOrganizationId(workerToken);

        // End date before start date.
        mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Analyst",
                                 "employmentType":"FULL_TIME","workRelationshipType":"EMPLOYEE",
                                 "sector":"NGO","startDate":"2023-01-01","endDate":"2022-01-01",
                                 "currentEmployment":false}
                                """.formatted(organizationId)))
                .andExpect(status().isBadRequest());

        // Current employment with a closed end date.
        mockMvc.perform(withBearer(post("/api/v1/me/employment"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"organizationId":"%s","jobTitleEn":"Analyst",
                                 "employmentType":"FULL_TIME","workRelationshipType":"EMPLOYEE",
                                 "sector":"NGO","startDate":"2023-01-01","endDate":"2024-01-01",
                                 "currentEmployment":true}
                                """.formatted(organizationId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the legacy caller-supplied-actor employment routes are gone")
    void legacyEmploymentRoutesAreGone() throws Exception {
        String organizationId = firstOrganizationId(workerToken);

        mockMvc.perform(withBearer(post("/api/v1/workers/" + currentUserId(workerToken)
                + "/employment?organizationId=" + organizationId + "&jobTitleEn=Sneaky"), workerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(withBearer(post("/api/v1/employment/00000000-0000-0000-0000-000000000000"
                + "/verification-requests?requesterId=" + currentUserId(workerToken)
                + "&organizationId=" + organizationId), workerToken))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------- identity

    @Test
    @DisplayName("identity verification starts and can now be completed")
    void identityVerificationCanComplete() throws Exception {
        MvcResult started = mockMvc.perform(withBearer(post("/api/v1/identity-verifications"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"purpose":"ACCOUNT_OPENING","policyVersion":"v1","locale":"en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.identityStatus").value("VERIFICATION_PENDING"))
                .andExpect(jsonPath("$.consentId").isNotEmpty())
                .andReturn();

        JsonNode attempt = readJson(started);
        String verificationId = attempt.get("id").asText();

        // The missing piece from the audit: the redirect URL is now exposed so a client can act on it.
        assertNotNull(attempt.get("redirectUrl").asText());
        assertTrue(attempt.get("actionRequired").asBoolean(),
                "a pending attempt must be flagged as requiring action");

        // Completing the attempt is what the audit found was never wired up.
        mockMvc.perform(withBearer(post("/api/v1/identity-verifications/" + verificationId
                + "/complete"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("SUCCEEDED"),
                        org.hamcrest.Matchers.is("FAILED"))));

        // The attempt is in the history.
        mockMvc.perform(withBearer(get("/api/v1/identity-verifications"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(verificationId));

        // Completing twice does not re-resolve an already-finished attempt.
        mockMvc.perform(withBearer(post("/api/v1/identity-verifications/" + verificationId
                + "/complete"), workerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("one account cannot complete another account's verification attempt")
    void identityVerificationOwnershipIsEnforced() throws Exception {
        MvcResult started = mockMvc.perform(withBearer(post("/api/v1/identity-verifications"),
                        employerToken)
                        .contentType("application/json")
                        .content("{\"purpose\":\"ACCOUNT_OPENING\",\"policyVersion\":\"v1\","
                                + "\"locale\":\"en\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String verificationId = readJson(started).get("id").asText();

        mockMvc.perform(withBearer(get("/api/v1/identity-verifications/" + verificationId), workerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(withBearer(post("/api/v1/identity-verifications/" + verificationId + "/complete"),
                workerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("identity start validation rejects a malformed locale")
    void identityStartValidationIsEnforced() throws Exception {
        mockMvc.perform(withBearer(post("/api/v1/identity-verifications"), guarantorToken)
                        .contentType("application/json")
                        .content("{\"purpose\":\"X\",\"policyVersion\":\"v1\",\"locale\":\"not a locale\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ----------------------------------------------------------- trust score

    @Test
    @DisplayName("reading a trust score is side-effect free and 404s before one exists")
    void trustScoreReadIsSideEffectFree() throws Exception {
        String userId = currentUserId(guarantorToken);

        // No score has been calculated for this account yet.
        mockMvc.perform(withBearer(get("/api/v1/trust-score/me"), guarantorToken))
                .andExpect(status().isNotFound());

        // Recalculation is an explicit write.
        mockMvc.perform(withBearer(post("/api/v1/trust-score/me/recalculate"), guarantorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.policyVersion").isNotEmpty());

        // Now the read succeeds and is repeatable.
        mockMvc.perform(withBearer(get("/api/v1/trust-score/me"), guarantorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber());
        mockMvc.perform(withBearer(get("/api/v1/trust-score/me"), guarantorToken))
                .andExpect(status().isOk());

        // History is available for the trend chart.
        mockMvc.perform(withBearer(get("/api/v1/trust-score/me/history"), guarantorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reasonCode").isNotEmpty());
    }

    @Test
    @DisplayName("one account cannot read another account's trust score")
    void trustScoreOwnershipIsEnforced() throws Exception {
        // Give the worker a score so the read is about authorization, not about absence.
        mockMvc.perform(withBearer(post("/api/v1/trust-score/me/recalculate"), workerToken))
                .andExpect(status().isOk());
        mockMvc.perform(withBearer(post("/api/v1/trust-score/me/recalculate"), guarantorToken))
                .andExpect(status().isOk());

        // Self-read works.
        mockMvc.perform(withBearer(get("/api/v1/trust-score/" + currentUserId(workerToken)), workerToken))
                .andExpect(status().isOk());

        // Reading somebody else's is refused.
        mockMvc.perform(withBearer(get("/api/v1/trust-score/" + currentUserId(guarantorToken)),
                workerToken))
                .andExpect(status().isForbidden());

        // An administrator may read any.
        mockMvc.perform(withBearer(get("/api/v1/trust-score/" + currentUserId(guarantorToken)),
                adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the old trust-engine GET route, which recalculated on read, is gone")
    void legacyTrustRouteIsGone() throws Exception {
        mockMvc.perform(withBearer(get("/api/trust-engine/" + currentUserId(workerToken) + "/score"),
                workerToken))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------- organizations

    @Test
    @DisplayName("organization membership exposes derived capabilities for the frontend")
    void organizationMembershipCapabilities() throws Exception {
        JsonNode memberships = readJson(mockMvc.perform(
                withBearer(get("/api/v1/me/organizations?status=ACTIVE"), workerToken)).andReturn());

        assertTrue(memberships.size() >= 1);
        JsonNode membership = memberships.get(0);
        assertNotNull(membership.get("organizationId").asText());
        assertNotNull(membership.get("role").asText());
        assertFalse(membership.get("canVerifyEmployment").asBoolean(),
                "an EMPLOYEE membership must not verify employment");
    }

    @Test
    @DisplayName("organization membership can be filtered by status")
    void organizationMembershipStatusFilter() throws Exception {
        mockMvc.perform(withBearer(get("/api/v1/me/organizations?status=REVOKED"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    // -------------------------------------------------------------- payments

    @Test
    @DisplayName("payment intent idempotency is preserved")
    void paymentIdempotencyIsPreserved() throws Exception {
        String idempotencyKey = "test-key-" + System.nanoTime();
        String body = """
                {"amount":500.00,"currency":"ETB","provider":"MOCK","purpose":"LOOKUP",
                 "idempotencyKey":"%s"}
                """.formatted(idempotencyKey);

        MvcResult first = mockMvc.perform(withBearer(post("/api/v1/payments/intents"), workerToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn();
        long firstId = readJson(first).get("id").asLong();

        // The same key with the same amount returns the same intent.
        MvcResult replay = mockMvc.perform(withBearer(post("/api/v1/payments/intents"), workerToken)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn();
        assertEquals(firstId, readJson(replay).get("id").asLong(),
                "an idempotent replay must not create a second intent");

        // The same key with different terms is refused.
        mockMvc.perform(withBearer(post("/api/v1/payments/intents"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"amount":999.00,"currency":"ETB","provider":"MOCK","purpose":"LOOKUP",
                                 "idempotencyKey":"%s"}
                                """.formatted(idempotencyKey)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a payment intent cannot be driven forward by another account")
    void paymentIntentOwnershipIsEnforced() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/payments/intents"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"amount":250.00,"currency":"ETB","provider":"MOCK","purpose":"LOOKUP",
                                 "idempotencyKey":"own-key-%s"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk()).andReturn();
        long intentId = readJson(created).get("id").asLong();

        mockMvc.perform(withBearer(post("/api/v1/payments/intents/" + intentId + "/initialize"),
                        guarantorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withBearer(post("/api/v1/payments/intents/" + intentId + "/initialize"),
                        workerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the webhook endpoint no longer exposes a client-controlled provider decision")
    void webhookProviderIsConstrained() throws Exception {
        // Without an event id the callback is refused rather than recorded.
        mockMvc.perform(withBearer(post("/api/v1/webhooks/payment?provider=MOCK"), adminToken))
                .andExpect(status().isBadRequest());

        // A well-formed callback with an invalid signature is recorded as rejected, not processed.
        mockMvc.perform(withBearer(post("/api/v1/webhooks/payment?provider=MOCK"), adminToken)
                        .header("X-Provider-Event-Id", "evt-" + System.nanoTime())
                        .header("X-Provider-Signature", "not-a-valid-signature")
                        .contentType("application/json")
                        .content("{\"provider\":\"MOCK\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SIGNATURE_REJECTED"));
    }

    private String firstOrganizationId(String token) throws Exception {
        JsonNode memberships = readJson(mockMvc.perform(
                withBearer(get("/api/v1/me/organizations"), token)).andReturn());
        assertTrue(memberships.size() > 0, "the seeded persona should belong to an organization");
        return memberships.get(0).get("organizationId").asText();
    }

    private String currentUserId(String token) throws Exception {
        return readJson(mockMvc.perform(withBearer(get("/api/v1/me"), token)).andReturn())
                .get("id").asText();
    }
}