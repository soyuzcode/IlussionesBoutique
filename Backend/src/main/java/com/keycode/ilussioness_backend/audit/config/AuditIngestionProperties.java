package com.keycode.ilussioness_backend.audit.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Bounded ingestion settings for the public audit endpoint. */
@Component
@Validated
@ConfigurationProperties(prefix = "audit.ingestion")
public class AuditIngestionProperties {

    @Min(512)
    @Max(65_536)
    private int maxBodyBytes = 4096;

    @Min(1)
    @Max(10_000)
    private int rateLimitPerMinute = 60;

    @Min(1)
    @Max(3600)
    private int rateLimitWindowSeconds = 60;

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) {
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }
}
