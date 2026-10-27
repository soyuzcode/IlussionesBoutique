package com.keycode.ilussioness_backend.audit.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generates a server-authoritative request correlation id and captures the
 * optional frontend audit session id.
 *
 * <p>Incoming {@code X-Request-Id} values are ignored/overwritten so a client
 * cannot choose server-derived context. The audit session id is only a
 * correlation aid, never an identity.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE =
            RequestIdFilter.class.getName() + ".requestId";
    public static final String SESSION_ID_ATTRIBUTE =
            RequestIdFilter.class.getName() + ".auditSessionId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String AUDIT_SESSION_HEADER = "X-Audit-Session-Id";

    private static final Pattern SESSION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String auditSessionId = request.getHeader(AUDIT_SESSION_HEADER);
        if (auditSessionId != null && SESSION_ID_PATTERN.matcher(auditSessionId).matches()) {
            request.setAttribute(SESSION_ID_ATTRIBUTE, auditSessionId);
        }

        MDC.put("requestId", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}
