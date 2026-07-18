package com.guru.erp.platform.security;

import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convenience accessor for the authenticated {@link AuthPrincipal}. Services use
 * this to stamp actor ids on audit rows and outbox events.
 */
@Component
public class CurrentUser {

    public Optional<AuthPrincipal> optional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /** The current principal, or throw 401 if unauthenticated. */
    public AuthPrincipal require() {
        return optional().orElseThrow(() -> new DomainException(ErrorCode.UNAUTHENTICATED));
    }

    public String requirePublicId() {
        return require().userPublicId();
    }
}
