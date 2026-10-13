package com.wastina.trust;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.wastina.trust.graph.PersonNodeRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class for full-context integration tests.
 *
 * <p>Runs against an in-memory H2 database with the local signed-JWT authentication mode enabled,
 * so the real {@code BearerTokenAuthenticationFilter} and the real {@code AccountStatusFilter}
 * execute exactly as they do in local development. Tokens are obtained through the real
 * development token endpoint rather than being fabricated in the test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** The trust graph is not part of these tests and needs a live Neo4j instance. */
    @MockBean
    @SuppressWarnings("unused")
    private PersonNodeRepository personNodeRepository;

    protected String workerToken;
    protected String employerToken;
    protected String guarantorToken;
    protected String adminToken;

    @BeforeEach
    void issueDevelopmentTokens() throws Exception {
        workerToken = tokenFor("worker");
        employerToken = tokenFor("employer");
        guarantorToken = tokenFor("guarantor");
        adminToken = tokenFor("admin");
    }

    protected String tokenFor(String persona) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/dev/auth/token")
                        .contentType("application/json")
                        .content("{\"persona\":\"" + persona + "\"}"))
                .andReturn();
        return readJson(result).get("accessToken").asText();
    }

    protected com.fasterxml.jackson.databind.JsonNode readJson(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body == null || body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }

    protected MockHttpServletRequestBuilder withBearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder get(String path) {
        return MockMvcRequestBuilders.get(path);
    }

    protected MockHttpServletRequestBuilder post(String path) {
        return MockMvcRequestBuilders.post(path);
    }

    protected MockHttpServletRequestBuilder patch(String path) {
        return MockMvcRequestBuilders.patch(path);
    }

    protected MockHttpServletRequestBuilder put(String path) {
        return MockMvcRequestBuilders.put(path);
    }

    protected MockHttpServletRequestBuilder delete(String path) {
        return MockMvcRequestBuilders.delete(path);
    }
}