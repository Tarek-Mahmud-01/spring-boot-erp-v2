package com.springboot.erp.platform.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Reads and writes the authentication cookies.
 *
 * <p>Access + refresh tokens live in {@code httpOnly} cookies so page JavaScript
 * (and therefore an XSS payload) can never read them. The CSRF token is a
 * separate, JS-readable cookie the SPA echoes back in a request header; the
 * server compares the two (double-submit). {@code SameSite=Strict} additionally
 * stops the browser sending the auth cookies on cross-site requests.
 */
@Component
public class AuthCookieService {

    private static final String PATH_API = "/api";
    private static final String PATH_REFRESH = "/api/auth";

    private final AuthCookieProperties.Cookie cfg;

    public AuthCookieService(AuthCookieProperties props) {
        this.cfg = props.cookie();
    }

    /** Set access (httpOnly), refresh (httpOnly, tighter path) and CSRF cookies. */
    public void writeAuthCookies(HttpServletResponse response, String accessToken,
                                 long accessTtlSeconds, String refreshToken,
                                 long refreshTtlSeconds, String csrfToken) {
        addCookie(response, build(cfg.accessName(), accessToken, PATH_API, accessTtlSeconds, true));
        addCookie(response, build(cfg.refreshName(), refreshToken, PATH_REFRESH, refreshTtlSeconds, true));
        // CSRF cookie is readable by JS (not httpOnly) so the SPA can echo it.
        addCookie(response, build(cfg.csrfName(), csrfToken, PATH_API, refreshTtlSeconds, false));
    }

    /** Expire all auth cookies (logout). */
    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, build(cfg.accessName(), "", PATH_API, 0, true));
        addCookie(response, build(cfg.refreshName(), "", PATH_REFRESH, 0, true));
        addCookie(response, build(cfg.csrfName(), "", PATH_API, 0, false));
    }

    public String readAccessToken(HttpServletRequest request) {
        return readCookie(request, cfg.accessName());
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, cfg.refreshName());
    }

    public String readCsrfCookie(HttpServletRequest request) {
        return readCookie(request, cfg.csrfName());
    }

    private ResponseCookie build(String name, String value, String path, long maxAgeSeconds,
                                 boolean httpOnly) {
        return ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(cfg.secure())
            .sameSite(cfg.sameSite())
            .path(path)
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }

    private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
