package com.guru.erp.modules.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email settings bound from {@code app.notification.*}. Mirrors the reference
 * SMTP_* settings. {@code fakeSend} logs the would-be email instead of sending
 * (useful in dev/staging with no mail server); an unset {@code host} means
 * "not configured" and sends are skipped with a log line, never an exception.
 */
@ConfigurationProperties(prefix = "app.notification.email")
public record NotificationProperties(
    String host,
    int port,
    String username,
    String password,
    boolean useTls,
    String fromEmail,
    String fromName,
    boolean fakeSend
) {
    public NotificationProperties {
        if (port <= 0) {
            port = 587;
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "Guru ERP";
        }
    }

    public boolean configured() {
        return host != null && !host.isBlank() && fromEmail != null && !fromEmail.isBlank();
    }
}
