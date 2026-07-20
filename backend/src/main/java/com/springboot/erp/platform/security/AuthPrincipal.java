package com.springboot.erp.platform.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The authenticated actor as carried in the SecurityContext. Backed by the JWT
 * claims — no DB hit per request. {@code authorities} are the flattened
 * permission codes granted via the user's roles, so {@code @PreAuthorize(
 * "hasAuthority('<perm.code>')")} works directly (ARCHITECTURE.md §2).
 *
 * @param userPublicId the user's ULID (also used as the JPA auditor)
 * @param username     login/display name
 * @param permissions  granted permission codes
 */
public record AuthPrincipal(String userPublicId, String username, List<String> permissions) {

    public Collection<? extends GrantedAuthority> authorities() {
        return permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();
    }
}
