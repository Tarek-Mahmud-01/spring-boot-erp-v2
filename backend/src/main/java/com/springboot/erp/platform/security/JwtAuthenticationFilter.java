package com.springboot.erp.platform.security;

import com.springboot.erp.platform.error.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer <jwt>} header, verifies it, and
 * populates the SecurityContext with an {@link AuthPrincipal} + its authorities
 * (permission codes). Stateless — no session. A missing/invalid token simply
 * leaves the context anonymous; the entry point / {@code @PreAuthorize} then
 * decide whether the endpoint is reachable.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER.length());
            try {
                AuthPrincipal principal = jwtService.parseAccessToken(token);
                var auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.authorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (DomainException ignored) {
                // Invalid/expired token → stay anonymous; entry point yields 401.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
