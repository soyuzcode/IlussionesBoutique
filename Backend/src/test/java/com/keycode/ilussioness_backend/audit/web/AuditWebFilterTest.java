package com.keycode.ilussioness_backend.audit.web;

import com.keycode.ilussioness_backend.audit.config.AuditIngestionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AuditWebFilterTest {

    private static final String AUDIT_PATH = "/api/audit/events";

    @Test
    void requestIdFilterGeneratesServerIdAndAcceptsCorrelationSession() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", AUDIT_PATH);
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "client-supplied-id");
        request.addHeader(RequestIdFilter.AUDIT_SESSION_HEADER, "session-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generatedId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        assertThat(generatedId).isNotBlank().isNotEqualTo("client-supplied-id");
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(generatedId);
        assertThat(request.getAttribute(RequestIdFilter.SESSION_ID_ATTRIBUTE))
                .isEqualTo("session-12345678");
    }

    @Test
    void requestIdFilterIgnoresInvalidSessionCorrelation() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", AUDIT_PATH);
        request.addHeader(RequestIdFilter.AUDIT_SESSION_HEADER, "invalid session with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(RequestIdFilter.SESSION_ID_ATTRIBUTE)).isNull();
    }

    @Test
    void sizeFilterAllowsBodyAtLimit() throws Exception {
        AuditIngestionProperties properties = new AuditIngestionProperties();
        properties.setMaxBodyBytes(64);
        AuditRequestSizeFilter filter = new AuditRequestSizeFilter(properties);

        MockHttpServletRequest request = auditRequest("{\"eventType\":\"UI_SESSION_STARTED\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isInstanceOf(CachedBodyHttpServletRequest.class);
        assertThat(response.getStatus()).isEqualTo(200);
        CachedBodyHttpServletRequest cached = (CachedBodyHttpServletRequest) chain.getRequest();
        assertThat(new String(cached.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("{\"eventType\":\"UI_SESSION_STARTED\"}");
    }

    @Test
    void sizeFilterRejectsOversizedBodyWithoutCallingTheChain() throws Exception {
        AuditIngestionProperties properties = new AuditIngestionProperties();
        properties.setMaxBodyBytes(32);
        AuditRequestSizeFilter filter = new AuditRequestSizeFilter(properties);

        MockHttpServletRequest request = auditRequest("x".repeat(33));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("payload_too_large");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void sizeFilterDoesNotApplyToOtherEndpoints() throws Exception {
        AuditIngestionProperties properties = new AuditIngestionProperties();
        properties.setMaxBodyBytes(32);
        AuditRequestSizeFilter filter = new AuditRequestSizeFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/customer");
        request.setContent("x".repeat(1024).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rateLimitAllowsRequestsUpToLimitAndRejectsTheNextOne() throws Exception {
        AuditIngestionProperties properties = new AuditIngestionProperties();
        properties.setRateLimitPerMinute(2);
        properties.setRateLimitWindowSeconds(60);
        AtomicLong clock = new AtomicLong(1_000);
        AuditRateLimitFilter filter = new AuditRateLimitFilter(properties, clock::get);

        assertThat(perform(filter, AUDIT_PATH).getStatus()).isEqualTo(200);
        assertThat(perform(filter, AUDIT_PATH).getStatus()).isEqualTo(200);

        MockHttpServletResponse limited = perform(filter, AUDIT_PATH);
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("rate_limited");
        assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void rateLimitResetsAfterTheWindow() throws Exception {
        AuditIngestionProperties properties = new AuditIngestionProperties();
        properties.setRateLimitPerMinute(1);
        properties.setRateLimitWindowSeconds(60);
        AtomicLong clock = new AtomicLong(1_000);
        AuditRateLimitFilter filter = new AuditRateLimitFilter(properties, clock::get);

        assertThat(perform(filter, AUDIT_PATH).getStatus()).isEqualTo(200);
        assertThat(perform(filter, AUDIT_PATH).getStatus()).isEqualTo(429);

        clock.addAndGet(60_000);
        assertThat(perform(filter, AUDIT_PATH).getStatus()).isEqualTo(200);
    }

    @Test
    void rateLimitDoesNotApplyToOtherEndpoints() throws Exception {
        AuditIngestionProperties properties = new AuditIngestionProperties();
        properties.setRateLimitPerMinute(1);
        AuditRateLimitFilter filter = new AuditRateLimitFilter(properties, () -> 1_000L);

        assertThat(perform(filter, "/api/customer").getStatus()).isEqualTo(200);
        assertThat(perform(filter, "/api/customer").getStatus()).isEqualTo(200);
        assertThat(perform(filter, "/api/customer").getStatus()).isEqualTo(200);
    }

    @Test
    void filtersDoNotThrowForNonAuditRequests() {
        AuditRateLimitFilter rateLimit = new AuditRateLimitFilter(
                new AuditIngestionProperties(),
                () -> 1_000L
        );
        AuditRequestSizeFilter size = new AuditRequestSizeFilter(new AuditIngestionProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customer");

        assertThatCode(() -> rateLimit.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        )).doesNotThrowAnyException();

        assertThatCode(() -> size.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        )).doesNotThrowAnyException();
    }

    private static MockHttpServletRequest auditRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", AUDIT_PATH);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static MockHttpServletResponse perform(
            AuditRateLimitFilter filter,
            String path
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
