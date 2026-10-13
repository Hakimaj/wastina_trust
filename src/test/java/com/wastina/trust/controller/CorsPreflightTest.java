package com.wastina.trust.controller;

import com.wastina.trust.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C-1: verifies the browser preflight contract that the audit found was missing outside the
 * development filter.
 *
 * <p>The previous CORS configuration lived only inside the development authentication filter, so a
 * deployed backend rejected every browser preflight and no frontend could call the API at all.
 */
class CorsPreflightTest extends AbstractIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    /** {@code getHeaders()} on MockHttpServletResponse needs an argument; use getHeader instead. */
    private static String header(MockHttpServletResponse response, String name) {
        return response.getHeader(name);
    }

    @Test
    @DisplayName("a preflight from the frontend origin is allowed without a token")
    void preflightIsAllowed() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.options("/api/v1/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "authorization,content-type,accept,x-request-id"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertEquals(ALLOWED_ORIGIN, header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNotNull(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
        assertNotNull(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    @DisplayName("X-Detected-Mime-Type is an allowed request header for document upload")
    void detectedMimeTypeHeaderIsAllowed() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(
                        MockMvcRequestBuilders.options("/api/v1/documents/upload-sessions")
                                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-detected-mime-type"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertTrue(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                .toLowerCase().contains("x-detected-mime-type"));
    }

    @Test
    @DisplayName("X-Request-Id is both an allowed request header and an exposed response header")
    void requestIdIsCorrelated() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.options("/api/v1/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-request-id"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertTrue(header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                .toLowerCase().contains("x-request-id"));
        assertTrue(header(response, HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)
                .toLowerCase().contains("x-request-id"));
    }

    @Test
    @DisplayName("an actual cross-origin GET carries the allow-origin header")
    void actualRequestIsAllowed() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(withBearer(
                        MockMvcRequestBuilders.get("/api/v1/me").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN),
                        workerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertEquals(ALLOWED_ORIGIN, header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("every response echoes a request id, generated or supplied")
    void requestIdIsEchoed() throws Exception {
        MockHttpServletRequestBuilder generated =
                MockMvcRequestBuilders.get("/actuator/health").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        assertNotNull(header(mockMvc.perform(generated).andReturn().getResponse(), "X-Request-Id"));

        MockHttpServletResponse supplied = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")
                        .header("X-Request-Id", "trace-abc-123"))
                .andReturn().getResponse();
        assertEquals("trace-abc-123", header(supplied, "X-Request-Id"));
    }

    @Test
    @DisplayName("an untrusted origin is not reflected")
    void untrustedOriginIsNotAllowed() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.options("/api/v1/me")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andReturn().getResponse();

        String allowOrigin = header(response, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        assertTrue(allowOrigin == null || !allowOrigin.contains("evil.example"),
                "an untrusted origin must not be reflected, got " + allowOrigin);
    }
}