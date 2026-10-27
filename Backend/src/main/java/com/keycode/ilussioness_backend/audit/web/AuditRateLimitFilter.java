package com.keycode.ilussioness_backend.audit.web;

import com.keycode.ilussioness_backend.audit.config.AuditIngestionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * Small in-memory fixed-window limiter for audit ingestion.
 *
 * <p>This is a best-effort abuse control, not a distributed rate limiter. It
 * is intentionally per-instance and resets on restart. The key is the socket
 * remote address; proxy headers are not trusted because they are
 * client-controlled.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuditRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final AuditIngestionProperties properties;
    private final LongSupplier clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuditRateLimitFilter(AuditIngestionProperties properties) {
        this(properties, System::currentTimeMillis);
    }

    AuditRateLimitFilter(AuditIngestionProperties properties, LongSupplier clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AuditRequestSizeFilter.isAuditIngestionRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long now = clock.getAsLong();
        long windowMillis = Math.max(1, properties.getRateLimitWindowSeconds()) * 1000L;
        int limit = Math.max(1, properties.getRateLimitPerMinute());

        cleanupIfNeeded(now, windowMillis);

        String key = clientKey(request);
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.windowStartMillis() >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStartMillis(), existing.count() + 1);
        });

        if (window.count() > limit) {
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(properties.getRateLimitWindowSeconds()));
            response.getWriter().write("{\"error\":\"rate_limited\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupIfNeeded(long now, long windowMillis) {
        if (windows.size() <= MAX_TRACKED_CLIENTS) {
            return;
        }
        windows.entrySet().removeIf(entry -> now - entry.getValue().windowStartMillis() >= windowMillis);
    }

    private static String clientKey(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private record Window(long windowStartMillis, int count) {
    }
}
