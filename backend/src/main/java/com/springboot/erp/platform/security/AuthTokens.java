package com.springboot.erp.platform.security;

import com.springboot.erp.modules.access.dto.AuthDtos.CurrentUserResponse;

/**
 * The raw credentials produced by a login/refresh, handed to the controller so
 * it can set the httpOnly auth cookies. The tokens themselves are never written
 * to the JSON response body — only the {@link #user} is surfaced to the client.
 */
public record AuthTokens(
    String accessToken,
    long accessTtlSeconds,
    String refreshToken,
    long refreshTtlSeconds,
    String csrfToken,
    CurrentUserResponse user
) {
}
