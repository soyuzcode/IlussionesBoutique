package com.keycode.ilussioness_backend.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level authentication tests.
 *
 * <p>They login through the real endpoint, reuse the issued JWT on protected
 * routes, and verify that invalid credentials, missing tokens, invalid tokens,
 * tampered tokens and SQL-injection payloads never grant access. Responses are
 * also checked for password/hash or internal implementation leakage.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.auth.jwt-secret=test-only-jwt-secret-0123456789abcdef0123456789abcdef",
        "app.auth.demo.email=admin@ilussiones.com",
        "app.auth.demo.password=TestDemoPass123!",
        "app.auth.demo.name=Admin",
        "app.auth.demo.role=ADMIN"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    private static final String EMAIL = "admin@ilussiones.com";
    private static final String PASSWORD = "TestDemoPass123!";
    private static final String PROTECTED_PATH = "/api/customer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successfulLoginReturnsABearerTokenAndPublicUser() throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.name").value("Admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain(PASSWORD)
                .doesNotContain("passwordHash")
                .doesNotContain("password")
                .doesNotContain("$2a$")
                .doesNotContain("$2b$")
                .doesNotContain("$2y$");
    }

    @Test
    void wrongPasswordIsRejectedWithGeneric401() throws Exception {
        mockMvc.perform(loginRequest(EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    void unknownEmailAndWrongPasswordShareTheSameResponse() throws Exception {
        String unknownEmailBody = mockMvc.perform(loginRequest("unknown@ilussiones.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String wrongPasswordBody = mockMvc.perform(loginRequest(EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmailBody).isEqualTo(wrongPasswordBody);
        assertThat(unknownEmailBody)
                .doesNotContain("unknown@ilussiones.com")
                .doesNotContain("not found")
                .doesNotContain("does not exist");
    }

    @Test
    void emptyCredentialsAreRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void blankAndMalformedEmailAreRejectedWith400() throws Exception {
        mockMvc.perform(loginRequest("   ", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());

        mockMvc.perform(loginRequest("not-an-email", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void malformedJsonBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"admin@ilussiones.com\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.reason").value("malformed_payload"));
    }

    @Test
    void unsupportedContentTypeReturns415WithoutGrantingAccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=admin@ilussiones.com"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("unsupported_media_type"));

        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sqlInjectionInPasswordDoesNotAuthenticate() throws Exception {
        mockMvc.perform(loginRequest(EMAIL, "' OR '1'='1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void sqlInjectionInEmailDoesNotAuthenticate() throws Exception {
        MvcResult result = mockMvc.perform(loginRequest("admin@ilussiones.com' OR '1'='1", PASSWORD))
                .andExpect(status().is4xxClientError())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("\"token\"");
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        MvcResult result = mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Exception")
                .doesNotContain("org.springframework")
                .doesNotContain("JWT");
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void protectedEndpointRejectsTamperedToken() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void validTokenAllowsAccessToProtectedEndpoint() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void validTokenAllowsAccessToProtectedAuditReadEndpoint() throws Exception {
        String token = obtainToken();

        mockMvc.perform(get("/api/audit/logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsDoNotGrantAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(loginRequest(EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String email, String password) throws Exception {
        Map<String, String> credentials = new LinkedHashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials));
    }

    private String obtainToken() throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
