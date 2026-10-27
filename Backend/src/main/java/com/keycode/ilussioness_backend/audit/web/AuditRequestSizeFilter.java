package com.keycode.ilussioness_backend.audit.web;

import com.keycode.ilussioness_backend.audit.config.AuditIngestionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Enforces the small request body budget for audit ingestion before the JSON
 * body is parsed. Oversized requests are rejected without attempting to log
 * them (which also prevents a flood from generating more audit rows).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class AuditRequestSizeFilter extends OncePerRequestFilter {

    static final String AUDIT_EVENTS_PATH = "/api/audit/events";

    private final AuditIngestionProperties properties;

    public AuditRequestSizeFilter(AuditIngestionProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isAuditIngestionRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        int maxBytes = properties.getMaxBodyBytes();

        if (request.getContentLengthLong() > maxBytes) {
            reject(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "payload_too_large");
            return;
        }

        byte[] body;
        try {
            body = request.getInputStream().readNBytes(maxBytes + 1);
        } catch (IOException ex) {
            reject(response, HttpServletResponse.SC_BAD_REQUEST, "malformed_payload");
            return;
        }

        if (body.length > maxBytes) {
            reject(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "payload_too_large");
            return;
        }

        filterChain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
    }

    static boolean isAuditIngestionRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "POST".equalsIgnoreCase(request.getMethod())
                && AUDIT_EVENTS_PATH.equals(path);
    }

    static void reject(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }
}
