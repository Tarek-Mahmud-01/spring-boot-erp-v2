package com.guru.erp.modules.access.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Auth request/response records (ARCHITECTURE.md §2 — DTOs as records). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {
    }

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {
    }

    /** The authenticated user as surfaced to the client (never the password hash). */
    public record CurrentUserResponse(
        String publicId,
        String username,
        String fullName,
        String email,
        List<String> permissions
    ) {
    }

    public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        CurrentUserResponse user
    ) {
    }
}
