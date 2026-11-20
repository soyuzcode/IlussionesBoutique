package com.keycode.ilussioness_backend.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:session-cookie;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.auth.allowed-origins=https://ilussiones-boutique-*-key-code1.vercel.app",
        "app.frontend-url=https://boutique.example.com"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionCookieApiTest {

    private static final String EMAIL = "admin@ilussiones.com";
    private static final String PASSWORD = "TestDemoPass123!";
    private static final String COOKIE = "ilussiones_session";

    @Autowired
    private MockMvc mockMvc;

    private MvcResult login(Boolean rememberMe) throws Exception {
        String remember = rememberMe == null ? "" : ",\"rememberMe\":" + rememberMe;
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"" + remember + "}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String setCookie(MvcResult result) {
        return result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    private static Cookie sessionCookie(MvcResult result) {
        String header = setCookie(result);
        String value = header.substring(header.indexOf('=') + 1, header.indexOf(';'));
        return new Cookie(COOKIE, value);
    }

    @Test
    void loginSetsHttpOnlySecureSessionCookieScopedToAuth() throws Exception {
        String header = setCookie(login(null));

        assertThat(header).startsWith(COOKIE + "=").contains("HttpOnly", "Secure", "SameSite=None", "Path=/api/auth");
        assertThat(header).contains("Max-Age=" + 14 * 24 * 3600);
    }

    @Test
    void loginWithoutRememberMeClearsTheCookie() throws Exception {
        assertThat(setCookie(login(false))).startsWith(COOKIE + "=;").contains("Max-Age=0");
    }

    @Test
    void sessionExchangesCookieForAccessTokenAndRenewsIt() throws Exception {
        Cookie cookie = sessionCookie(login(true));

        MvcResult result = mockMvc.perform(post("/api/auth/session").header("X-Requested-With", "XMLHttpRequest").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        assertThat(setCookie(result)).startsWith(COOKIE + "=").doesNotContain("Max-Age=0");
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get("/api/customer").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void sessionWithoutValidCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/session").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("no_session"));
        mockMvc.perform(post("/api/auth/session").header("X-Requested-With", "XMLHttpRequest").cookie(new Cookie(COOKIE, "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionCookieIsNotAcceptedAsBearerToken() throws Exception {
        Cookie cookie = sessionCookie(login(true));

        mockMvc.perform(get("/api/customer").header(HttpHeaders.AUTHORIZATION, "Bearer " + cookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenIsNotAcceptedAsSessionCookie() throws Exception {
        String token = JsonPath.read(login(false).getResponse().getContentAsString(), "$.token");

        mockMvc.perform(post("/api/auth/session").header("X-Requested-With", "XMLHttpRequest").cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cookieEndpointsRequireTheScriptHeader() throws Exception {
        Cookie cookie = sessionCookie(login(true));

        mockMvc.perform(post("/api/auth/session").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("csrf_header_required"));
        mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutClearsTheCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(setCookie(result)).startsWith(COOKIE + "=;").contains("Max-Age=0");
    }

    @Test
    void credentialedCorsOnlyForConfiguredFrontendOrigins() throws Exception {
        for (String origin : new String[]{
                "https://boutique.example.com",
                "https://ilussiones-boutique-git-feature-key-code1.vercel.app"}) {
            mockMvc.perform(options("/api/auth/session")
                            .header(HttpHeaders.ORIGIN, origin)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        mockMvc.perform(options("/api/customer")
                        .header(HttpHeaders.ORIGIN, "https://evil.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

        mockMvc.perform(options("/api/auth/session")
                        .header(HttpHeaders.ORIGIN, "https://evil.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
