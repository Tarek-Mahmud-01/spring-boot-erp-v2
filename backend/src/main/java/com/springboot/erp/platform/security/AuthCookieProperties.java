package com.springboot.erp.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication hardening settings bound from {@code app.auth.*}.
 *
 * <p>Tokens are delivered as httpOnly cookies (XSS cannot read them). The CSRF
 * cookie is the one non-httpOnly value: the SPA echoes it back in a header so
 * the server can confirm the request originates from our own page (double-submit
 * cookie pattern), which — together with {@code SameSite=Strict} — defends the
 * cookie-borne session against CSRF.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthCookieProperties(
    Cookie cookie,
    LoginThrottle loginThrottle,
    boolean allowInsecureSecret
) {

    public record Cookie(
        boolean secure,
        String sameSite,
        String accessName,
        String refreshName,
        String csrfName
    ) {
        public Cookie {
            if (sameSite == null || sameSite.isBlank()) {
                sameSite = "Strict";
            }
            if (accessName == null || accessName.isBlank()) {
                accessName = "ERP_AT";
            }
            if (refreshName == null || refreshName.isBlank()) {
                refreshName = "ERP_RT";
            }
            if (csrfName == null || csrfName.isBlank()) {
                csrfName = "ERP_CSRF";
            }
        }
    }

    public record LoginThrottle(
        int maxAttempts,
        int windowMinutes,
        int lockMinutes
    ) {
        public LoginThrottle {
            if (maxAttempts <= 0) {
                maxAttempts = 5;
            }
            if (windowMinutes <= 0) {
                windowMinutes = 15;
            }
            if (lockMinutes <= 0) {
                lockMinutes = 15;
            }
        }
    }
}
