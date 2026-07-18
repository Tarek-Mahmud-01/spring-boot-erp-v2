package com.guru.erp.modules.access.dto;

import java.time.Instant;
import java.util.List;

/** User read DTOs (ARCHITECTURE.md §2 — records). */
public final class UserDtos {

    private UserDtos() {
    }

    /** Row shape for the server-driven users list. */
    public record UserRow(
        String publicId,
        String username,
        String fullName,
        String email,
        boolean active,
        List<String> roles,
        Instant createdAt
    ) {
    }
}
