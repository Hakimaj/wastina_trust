package com.wastina.trust.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.wastina.trust.AbstractIntegrationTest;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that local development authentication is a genuine JWT bearer flow rather than a
 * request filter that injects an authenticated principal.
 *
 * <p>The negative cases matter most: a missing, malformed, wrongly signed, expired, wrongly
 * issued or wrongly audienced token must all be rejected with 401.
 */
class LocalJwtAuthenticationTest extends AbstractIntegrationTest {

    private static final String SECRET = "test-only-signing-key-at-least-32-characters-long";
    private static final String ISSUER = "http://localhost/wastina-test-issuer";

    @Test
    @DisplayName("development token endpoint is anonymous and lists only the fixed personas")
    void tokenEndpointIsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/dev/auth/personas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0].persona").value("worker"))
                .andExpect(jsonPath("$[0].role").value("ROLE_WORKER"));
    }

    @Test
    @DisplayName("a token issued for a known persona authenticates a protected endpoint")
    void issuedTokenAuthenticates() throws Exception {
        MvcResult result = mockMvc.perform(withBearer(get("/api/v1/me"), workerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = readJson(result);
        assertEquals("ACTIVE", body.get("account_status").asText());
        assertTrue(body.get("roles").toString().contains("ROLE_WORKER"));
    }

    @Test
    @DisplayName("no token at all is rejected with 401 - this is not an authentication bypass")
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token signed with the wrong key is rejected with 401")
    void wrongSignatureIsRejected() throws Exception {
        String forged = signWith("a-completely-different-signing-key-32-chars-min");

        mockMvc.perform(withBearer(get("/api/v1/me"), forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token signed with a tampered payload is rejected with 401")
    void tamperedTokenIsRejected() throws Exception {
        String valid = signWith(SECRET);

        mockMvc.perform(withBearer(get("/api/v1/me"), valid.substring(0, valid.length() - 3) + "AAA"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an expired token is rejected with 401")
    void expiredTokenIsRejected() throws Exception {
        String expired = buildToken(SECRET, ISSUER, Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600), "wastina-api");

        mockMvc.perform(withBearer(get("/api/v1/me"), expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token from an untrusted issuer is rejected with 401")
    void foreignIssuerIsRejected() throws Exception {
        String foreign = buildToken(SECRET, "https://evil.example/issuer",
                Instant.now(), Instant.now().plusSeconds(600), "wastina-api");

        mockMvc.perform(withBearer(get("/api/v1/me"), foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token for a different audience is rejected with 401")
    void wrongAudienceIsRejected() throws Exception {
        String wrongAudience = buildToken(SECRET, ISSUER, Instant.now(),
                Instant.now().plusSeconds(600), "some-other-api");

        mockMvc.perform(withBearer(get("/api/v1/me"), wrongAudience))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a garbage Authorization header is rejected with 401")
    void garbageHeaderIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a valid token for an unknown subject cannot reach a protected endpoint")
    void unknownSubjectIsRejectedByAccountStatusFilter() throws Exception {
        String unknownSubject = buildToken(SECRET, ISSUER, Instant.now(),
                Instant.now().plusSeconds(600), "wastina-api", "dev-someone-unknown", List.of("ROLE_ADMIN"));

        mockMvc.perform(withBearer(get("/api/v1/me"), unknownSubject))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("each persona receives exactly one persisted role and the matching /me projection")
    void personasCarryDistinctRoles() throws Exception {
        assertRoleFor(workerToken, "ROLE_WORKER");
        assertRoleFor(employerToken, "ROLE_EMPLOYER");
        assertRoleFor(guarantorToken, "ROLE_GUARANTOR");
        assertRoleFor(adminToken, "ROLE_ADMIN");

        assertNotEquals(workerToken, employerToken);

        // Only the admin persona may ever hold ROLE_ADMIN.
        assertTrue(readJson(mockMvc.perform(withBearer(get("/api/v1/me"), adminToken)).andReturn())
                .get("roles").toString().contains("ROLE_ADMIN"));
        for (String nonAdmin : new String[]{workerToken, employerToken, guarantorToken}) {
            assertFalse(readJson(mockMvc.perform(withBearer(get("/api/v1/me"), nonAdmin)).andReturn())
                    .get("roles").toString().contains("ROLE_ADMIN"),
                    "a non-admin persona must not hold ROLE_ADMIN");
        }
    }

    @Test
    @DisplayName("an unknown persona is rejected with 400, not a fabricated identity")
    void unknownPersonaIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/dev/auth/token")
                        .contentType("application/json")
                        .content("{\"persona\":\"superadmin\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/dev/auth/token")
                        .contentType("application/json")
                        .content("{\"persona\":\"worker\",\"subject\":\"attacker\",\"roles\":[\"ROLE_ADMIN\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a token cannot be used to reach another account's resources")
    void tokensAreNotInterchangeableAcrossAccounts() throws Exception {
        String workerId = currentUserId(workerToken);
        String employerId = currentUserId(employerToken);

        // The worker asking for the employer's profile must be refused as cross-account.
        mockMvc.perform(withBearer(get("/api/employers/" + employerId + "/profile"), workerToken))
                .andExpect(status().isForbidden());

        // The worker asking for its own (nonexistent) employer profile is allowed and 404s.
        mockMvc.perform(withBearer(get("/api/employers/" + workerId + "/profile"), workerToken))
                .andExpect(status().isNotFound());

        // And the employer can read its own.
        mockMvc.perform(withBearer(get("/api/employers/" + employerId + "/profile"), employerToken))
                .andExpect(status().isOk());
    }

    private String currentUserId(String token) throws Exception {
        return readJson(mockMvc.perform(withBearer(get("/api/v1/me"), token)).andReturn())
                .get("id").asText();
    }

    private void assertRoleFor(String token, String expectedRole) throws Exception {
        JsonNode roles = readJson(mockMvc.perform(withBearer(get("/api/v1/me"), token))
                .andExpect(status().isOk()).andReturn()).get("roles");
        assertEquals(1, roles.size(), "expected exactly one role, got " + roles);
        assertEquals(expectedRole, roles.get(0).asText());
    }

    private String signWith(String secret) throws Exception {
        return buildToken(secret, ISSUER, Instant.now(), Instant.now().plusSeconds(600), "wastina-api");
    }

    private String buildToken(String secret, String issuer, Instant issuedAt, Instant expiresAt,
                              String audience) throws Exception {
        return buildToken(secret, issuer, issuedAt, expiresAt, audience, "dev-worker", List.of("ROLE_WORKER"));
    }

    private String buildToken(String secret, String issuer, Instant issuedAt, Instant expiresAt,
                              String audience, String subject, List<String> roles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(List.of(audience))
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("roles", roles)
                .build();

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).build(), claims);
        signedJwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }
}