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
 * Regression tests for the IDOR and authorization findings: every endpoint that previously accepted
 * a caller-supplied user, payer, requester, owner or actor identifier.
 */
class OwnershipAuthorizationTest extends AbstractIntegrationTest {

    // ---------------------------------------------------------------- payments

    @Test
    @DisplayName("payments are scoped to the payer; payerId can no longer be supplied")
    void paymentsAreScopedToPayer() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/me/payments"), workerToken)
                        .contentType("application/json")
                        .content("{\"amount\":150.00,\"currency\":\"ETB\",\"purpose\":\"LOOKUP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payerId").value(currentUserId(workerToken)))
                .andReturn();

        long paymentId = readJson(created).get("id").asLong();

        // The payer can read their own payment.
        mockMvc.perform(withBearer(get("/api/v1/me/payments/" + paymentId), workerToken))
                .andExpect(status().isOk());

        // Another authenticated user cannot.
        mockMvc.perform(withBearer(get("/api/v1/me/payments/" + paymentId), guarantorToken))
                .andExpect(status().isForbidden());

        // Supplying a foreign payerId is now impossible: the field is gone.
        mockMvc.perform(withBearer(post("/api/v1/me/payments"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"payerId":"%s","amount":10.00,"purpose":"spoofed"}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("the legacy all-payments endpoint is gone")
    void legacyPayerListIsGone() throws Exception {
        mockMvc.perform(withBearer(get("/api/payments/payer/" + currentUserId(workerToken)), workerToken))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------- guarantee requests

    @Test
    @DisplayName("guarantee requests are scoped to the requester")
    void guaranteeRequestsAreScopedToRequester() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/me/guarantee-requests"), workerToken)
                        .contentType("application/json")
                        .content("{\"guarantorPhone\":\"+251911234567\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long requestId = readJson(created).get("id").asLong();

        mockMvc.perform(withBearer(get("/api/v1/me/guarantee-requests/" + requestId), workerToken))
                .andExpect(status().isOk());

        // Another user gets 404, not 403, so existence is not disclosed.
        mockMvc.perform(withBearer(get("/api/v1/me/guarantee-requests/" + requestId), employerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the legacy requester-scoped guarantee request route is gone")
    void legacyGuaranteeRequestRouteIsGone() throws Exception {
        mockMvc.perform(withBearer(get("/api/guarantee-requests/requester/"
                + currentUserId(workerToken)), workerToken))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------- guarantees

    @Test
    @DisplayName("a guarantee is persisted and identified by a real reference")
    void guaranteeIsPersisted() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/guarantees"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"guarantorId":"%s","amount":25000.00,"currency":"ETB",
                                 "type":"RENT","purpose":"April rent"}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING_ACCEPTANCE"))
                .andExpect(jsonPath("$.principalId").value(currentUserId(workerToken)))
                .andExpect(jsonPath("$.guarantorId").value(currentUserId(guarantorToken)))
                .andExpect(jsonPath("$.contractHash").isNotEmpty())
                .andReturn();

        String reference = readJson(created).get("publicReference").asText();
        assertTrue(reference.startsWith("WST-"), "expected a public reference, got " + reference);

        // It is genuinely persisted: the reference resolves to the same record.
        JsonNode reread = readJson(mockMvc.perform(
                withBearer(get("/api/v1/guarantees/" + reference), workerToken)).andReturn());
        assertEquals(reference, reread.get("publicReference").asText());
        assertEquals(25000.00, reread.get("amount").asDouble(), 0.001);
        assertEquals("ETB", reread.get("currency").asText());
        assertEquals("RENT", reread.get("type").asText());
    }

    @Test
    @DisplayName("the caller is always the principal, never a body-supplied principalId")
    void guaranteePrincipalCannotBeSpoofed() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/guarantees"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"principalId":"%s","guarantorId":"%s","amount":1000.00,
                                 "type":"LOAN"}
                                """.formatted(currentUserId(employerToken), currentUserId(guarantorToken))))
                .andExpect(status().isCreated())
                .andReturn();

        assertEquals(currentUserId(workerToken),
                readJson(created).get("principalId").asText(),
                "a body-supplied principalId must be ignored");
    }

    @Test
    @DisplayName("a non-party cannot read a guarantee")
    void nonPartyCannotReadGuarantee() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/guarantees"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"guarantorId":"%s","amount":500.00,"type":"SERVICE"}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated()).andReturn();

        String reference = readJson(created).get("publicReference").asText();

        // The employer is not a party.
        mockMvc.perform(withBearer(get("/api/v1/guarantees/" + reference), employerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("only the guarantor can accept a guarantee")
    void onlyGuarantorCanAccept() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/guarantees"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"guarantorId":"%s","amount":750.00,"type":"COMMERCIAL"}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated()).andReturn();

        String reference = readJson(created).get("publicReference").asText();

        // The principal cannot accept their own request.
        mockMvc.perform(withBearer(post("/api/v1/guarantees/" + reference + "/accept"), workerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withBearer(post("/api/v1/guarantees/" + reference + "/accept"), guarantorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("a zero or negative guarantee amount is rejected")
    void guaranteeAmountIsValidated() throws Exception {
        mockMvc.perform(withBearer(post("/api/v1/guarantees"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"guarantorId":"%s","amount":0,"type":"RENT"}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------------- contracts

    @Test
    @DisplayName("contracts are scoped to their parties")
    void contractsAreScopedToParties() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/contracts"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"guarantorId":"%s","liabilityAmount":5000.00}
                                """.formatted(currentUserId(guarantorToken))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workerId").value(currentUserId(workerToken)))
                .andExpect(jsonPath("$.termsHash").isNotEmpty())
                .andReturn();

        long contractId = readJson(created).get("id").asLong();

        mockMvc.perform(withBearer(get("/api/v1/contracts/" + contractId), workerToken))
                .andExpect(status().isOk());

        // Not a party.
        mockMvc.perform(withBearer(get("/api/v1/contracts/" + contractId), employerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the unrestricted contract list and the worker-scoped route are gone")
    void unrestrictedContractRoutesAreGone() throws Exception {
        // /api/contracts moved under /api/v1 and is now scoped to the caller.
        mockMvc.perform(withBearer(get("/api/contracts"), workerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(withBearer(get("/api/contracts/worker/" + currentUserId(guarantorToken)),
                        workerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("termsHash is computed server-side, not accepted from the client")
    void contractTermsHashCannotBeSpoofed() throws Exception {
        MvcResult created = mockMvc.perform(withBearer(post("/api/v1/contracts"), workerToken)
                        .contentType("application/json")
                        .content("""
                                {"workerId":"%s","guarantorId":"%s","liabilityAmount":1000.00,
                                 "termsHash":"i-made-this-up"}
                                """.formatted(currentUserId(employerToken),
                                currentUserId(guarantorToken))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode contract = readJson(created);
        assertEquals(currentUserId(workerToken), contract.get("workerId").asText(),
                "workerId must come from the token");
        assertFalse("i-made-this-up".equals(contract.get("termsHash").asText()),
                "a client-supplied termsHash must not be echoed back");
    }

    // -------------------------------------------------------------- documents

    @Test
    @DisplayName("document download authorization is derived server-side")
    void documentDownloadIsAuthorizedServerSide() throws Exception {
        MvcResult session = mockMvc.perform(withBearer(post("/api/v1/documents/upload-sessions"), workerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"IDENTITY_DOCUMENT\",\"purpose\":\"Onboarding\","
                                + "\"declaredMimeType\":\"application/pdf\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allowedSize").isNumber())
                .andExpect(jsonPath("$.allowedMimeTypes").isArray())
                .andReturn();

        JsonNode sessionBody = readJson(session);
        String sessionId = sessionBody.get("id").asText();
        String documentId = sessionBody.get("documentId").asText();

        // This is the exact exploit from the audit: a non-owner appending ?authorized=true to
        // download somebody else's document. The query parameters are now ignored entirely, so
        // authorization is decided from the real owner and the call is refused.
        mockMvc.perform(withBearer(get("/api/v1/documents/" + documentId
                + "/download-url?requesterId=" + currentUserId(employerToken) + "&authorized=true"),
                employerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // And the supported route refuses a non-owner with 403 rather than trusting a query flag.
        mockMvc.perform(withBearer(get("/api/v1/documents/" + documentId + "/download-url"), employerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a document upload is quarantined, scanned and then published")
    void documentUploadIsScannedBeforePublishing() throws Exception {
        MvcResult session = mockMvc.perform(withBearer(post("/api/v1/documents/upload-sessions"), workerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"REPORT\",\"purpose\":\"Test report\","
                                + "\"declaredMimeType\":\"text/plain\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = readJson(session).get("id").asText();

        MvcResult completed = mockMvc.perform(
                        withBearer(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .multipart("/api/v1/documents/upload-sessions/" + sessionId + "/complete")
                                .file(new org.springframework.mock.web.MockMultipartFile(
                                        "file", "report.txt", "text/plain", "hello".getBytes()))
                                .param("detectedMimeType", "text/plain"),
                        workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.scanStatus").value("PASSED"))
                .andExpect(jsonPath("$.contentHash").isNotEmpty())
                .andExpect(jsonPath("$.downloadable").value(true))
                .andReturn();

        String documentId = readJson(completed).get("id").asText();

        // The owner obtains a download URL...
        mockMvc.perform(withBearer(get("/api/v1/documents/" + documentId + "/download-url"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value(300));

        // ...and nobody else can.
        mockMvc.perform(withBearer(get("/api/v1/documents/" + documentId + "/download-url"), adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an upload whose content type is not allowed is refused before storage")
    void documentUploadRejectsDisallowedMimeType() throws Exception {
        MvcResult session = mockMvc.perform(withBearer(post("/api/v1/documents/upload-sessions"), workerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"REPORT\",\"purpose\":\"Test\","
                                + "\"declaredMimeType\":\"application/pdf\"}"))
                .andExpect(status().isCreated()).andReturn();

        String sessionId = readJson(session).get("id").asText();

        mockMvc.perform(
                        withBearer(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .multipart("/api/v1/documents/upload-sessions/" + sessionId + "/complete")
                                .file(new org.springframework.mock.web.MockMultipartFile(
                                        "file", "x.exe", "application/x-msdownload",
                                        "MZ".getBytes()))
                                .param("detectedMimeType", "application/x-msdownload"),
                        workerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("one account cannot complete another account's upload session")
    void documentUploadSessionOwnershipIsEnforced() throws Exception {
        MvcResult session = mockMvc.perform(withBearer(post("/api/v1/documents/upload-sessions"), workerToken)
                        .contentType("application/json")
                        .content("{\"type\":\"REPORT\",\"purpose\":\"Ownership test\","
                                + "\"declaredMimeType\":\"text/plain\"}"))
                .andExpect(status().isCreated()).andReturn();

        String sessionId = readJson(session).get("id").asText();

        mockMvc.perform(
                        withBearer(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .multipart("/api/v1/documents/upload-sessions/" + sessionId + "/complete")
                                .file(new org.springframework.mock.web.MockMultipartFile(
                                        "file", "x.txt", "text/plain", "data".getBytes()))
                                .param("detectedMimeType", "text/plain"),
                        employerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("document listing is scoped to the owner and paged")
    void documentListingIsScopedAndPaged() throws Exception {
        MvcResult result = mockMvc.perform(
                        withBearer(get("/api/v1/documents?page=0&size=5"), workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andReturn();

        JsonNode page = readJson(result);
        assertNotNull(page.get("totalElements"));
        assertTrue(page.get("totalElements").asLong() >= 1);
    }

    private String currentUserId(String token) throws Exception {
        return readJson(mockMvc.perform(withBearer(get("/api/v1/me"), token)).andReturn())
                .get("id").asText();
    }
}