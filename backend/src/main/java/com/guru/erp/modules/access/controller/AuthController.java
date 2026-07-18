package com.guru.erp.modules.access.controller;

import com.guru.erp.modules.access.dto.AuthDtos.CurrentUserResponse;
import com.guru.erp.modules.access.dto.AuthDtos.LoginRequest;
import com.guru.erp.modules.access.dto.AuthDtos.LoginResponse;
import com.guru.erp.modules.access.dto.AuthDtos.RefreshRequest;
import com.guru.erp.modules.access.service.AuthService;
import com.guru.erp.platform.security.CurrentUser;
import com.guru.erp.platform.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints (ARCHITECTURE.md §2 — thin controller). {@code /login} and
 * {@code /refresh} are public (see SecurityConfig); {@code /me} requires a
 * valid token. All business logic is in AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, JwtService jwtService, CurrentUser currentUser) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        String userPublicId = jwtService.parseRefreshToken(request.refreshToken());
        return authService.refresh(userPublicId);
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return authService.currentUser(currentUser.requirePublicId());
    }
}
