package com.springboot.erp.platform.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Supplies the current actor's public id (ULID) to JPA auditing so
 * {@code created_by}/{@code updated_by} are filled from the security context.
 * Falls back to {@code "system"} for unauthenticated work (migrations, jobs).
 */
@Component
public class SecurityContextAuditorAware implements AuditorAware<String> {

    static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String s && "anonymousUser".equals(s)) {
            return Optional.of(SYSTEM);
        }
        if (auth.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal.userPublicId());
        }
        return Optional.of(auth.getName());
    }
}
