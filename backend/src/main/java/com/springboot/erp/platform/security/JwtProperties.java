package com.springboot.erp.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings bound from {@code app.jwt.*} (see application.yml). The secret
 * must be at least 32 bytes for HS256; supply it via the {@code JWT_SECRET}
 * environment variable in real environments.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTtlMinutes,
    long refreshTtlDays,
    String issuer
) {
    public JwtProperties {
        if (accessTtlMinutes <= 0) {
            accessTtlMinutes = 30;
        }
        if (refreshTtlDays <= 0) {
            refreshTtlDays = 14;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "guru-erp";
        }
    }
}
