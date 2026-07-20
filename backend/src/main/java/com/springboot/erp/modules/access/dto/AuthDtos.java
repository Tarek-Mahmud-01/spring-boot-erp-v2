package com.springboot.erp.modules.access.dto;

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

    /** The authenticated user as surfaced to the client (never the password hash). */
    public record CurrentUserResponse(
        String publicId,
        String username,
        String fullName,
        String email,
        List<String> permissions
    ) {
    }

    /**
     * Login/refresh JSON body. Tokens are NOT returned here — they are set as
     * httpOnly cookies. The client receives only the user + access lifetime.
     */
    public record LoginResponse(
        long expiresInSeconds,
        CurrentUserResponse user
    ) {
    }
}
