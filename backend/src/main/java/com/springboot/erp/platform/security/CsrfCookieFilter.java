package com.springboot.erp.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Double-submit CSRF guard for cookie-authenticated, state-changing requests.
 *
 * <p>Because the auth token rides in a cookie, the browser attaches it
 * automatically — including on cross-site form posts. To prove a mutating
 * request really came from our SPA, the client must echo the CSRF cookie value
 * in the {@code X-CSRF-Token} header; the two must match. Safe methods
 * (GET/HEAD/OPTIONS) and the public login/refresh endpoints are exempt. Requests
 * that authenticate via a {@code Bearer} header (API clients, not cookies) are
 * also exempt, since CSRF only applies to ambient cookie credentials.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> EXEMPT_PATHS = Set.of("/api/auth/login", "/api/auth/refresh");

    private final AuthCookieService cookieService;

    public CsrfCookieFilter(AuthCookieService cookieService) {
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (requiresCsrfCheck(request)) {
            String cookie = cookieService.readCsrfCookie(request);
            String header = request.getHeader(CSRF_HEADER);
            if (!StringUtils.hasText(cookie) || !cookie.equals(header)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
                        + "\"detail\":\"Missing or invalid CSRF token\",\"code\":\"csrf\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean requiresCsrfCheck(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (EXEMPT_PATHS.contains(path)) {
            return false;
        }
        // Only cookie-authenticated requests need CSRF protection. A Bearer header
        // is not sent automatically by the browser, so it can't be forged cross-site.
        return StringUtils.hasText(cookieService.readAccessToken(request));
    }
}
