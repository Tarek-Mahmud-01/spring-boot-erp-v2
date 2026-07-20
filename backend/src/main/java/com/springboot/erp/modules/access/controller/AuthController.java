package com.springboot.erp.modules.access.controller;

import com.springboot.erp.modules.access.dto.AuthDtos.CurrentUserResponse;
import com.springboot.erp.modules.access.dto.AuthDtos.LoginRequest;
import com.springboot.erp.modules.access.dto.AuthDtos.LoginResponse;
import com.springboot.erp.modules.access.service.AuthService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.AuthCookieService;
import com.springboot.erp.platform.security.AuthTokens;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.security.JwtService;
import com.springboot.erp.platform.security.LoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints (ARCHITECTURE.md §2 — thin controller). {@code /login} and
 * {@code /refresh} are public (see SecurityConfig); {@code /me} and
 * {@code /logout} require a valid session.
 *
 * <p>Tokens are delivered as httpOnly cookies — never in the JSON body — so page
 * JS/XSS cannot read them. Login is brute-force throttled per ip+username, and
 * refresh tokens are single-use (rotated + revocable server-side).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final AuthCookieService cookieService;
    private final LoginThrottle loginThrottle;

    public AuthController(AuthService authService, JwtService jwtService, CurrentUser currentUser,
                          AuthCookieService cookieService, LoginThrottle loginThrottle) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.cookieService = cookieService;
        this.loginThrottle = loginThrottle;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest, HttpServletResponse response) {
        String ip = clientIp(httpRequest);
        loginThrottle.checkNotLocked(ip, request.username());
        try {
            AuthTokens tokens = authService.login(request.username(), request.password());
            loginThrottle.reset(ip, request.username());
            return setCookiesAndBody(tokens, response);
        } catch (DomainException e) {
            if (e.errorCode() == ErrorCode.INVALID_CREDENTIALS) {
                loginThrottle.recordFailure(ip, request.username());
            }
            throw e;
        }
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(HttpServletRequest httpRequest, HttpServletResponse response) {
        String refreshToken = cookieService.readRefreshToken(httpRequest);
        if (!StringUtils.hasText(refreshToken)) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "No refresh token");
        }
        JwtService.RefreshClaims claims = jwtService.parseRefreshToken(refreshToken);
        AuthTokens tokens = authService.refresh(claims.userPublicId(), claims.jti());
        return setCookiesAndBody(tokens, response);
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        authService.logout(currentUser.requirePublicId());
        cookieService.clearAuthCookies(response);
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return authService.currentUser(currentUser.requirePublicId());
    }

    private LoginResponse setCookiesAndBody(AuthTokens tokens, HttpServletResponse response) {
        cookieService.writeAuthCookies(
            response,
            tokens.accessToken(), tokens.accessTtlSeconds(),
            tokens.refreshToken(), tokens.refreshTtlSeconds(),
            tokens.csrfToken());
        return new LoginResponse(tokens.accessTtlSeconds(), tokens.user());
    }

    /** Prefer the proxy-forwarded client IP when present, else the socket peer. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
