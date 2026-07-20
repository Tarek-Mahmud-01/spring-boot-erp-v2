package com.springboot.erp.modules.access.dto;

import java.time.Instant;
import java.util.List;

/** Role read DTOs (ARCHITECTURE.md §2 — records). */
public final class RoleDtos {

    private RoleDtos() {
    }

    /** Row shape for the server-driven roles list. */
    public record RoleRow(
        String publicId,
        String code,
        String name,
        String description,
        boolean system,
        List<String> permissions,
        Instant createdAt
    ) {
    }
}
