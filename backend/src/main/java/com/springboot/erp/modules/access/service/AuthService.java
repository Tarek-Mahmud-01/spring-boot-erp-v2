package com.springboot.erp.modules.access.service;

import com.springboot.erp.modules.access.domain.User;
import com.springboot.erp.modules.access.dto.AuthDtos.CurrentUserResponse;
import com.springboot.erp.modules.access.dto.AuthDtos.LoginResponse;
import com.springboot.erp.modules.access.mapper.UserMapper;
import com.springboot.erp.modules.access.repository.UserRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.AuthPrincipal;
import com.springboot.erp.platform.security.AuthTokens;
import com.springboot.erp.platform.security.JwtService;
import com.springboot.erp.platform.security.RefreshTokenStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication use-cases (ARCHITECTURE.md §2 — service holds business rules).
 * Verifies credentials, issues JWTs embedding the flattened permission set, and
 * resolves the current user. Kept under the 250-line cap; split by use-case if
 * it grows.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, UserMapper userMapper,
                       RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional(readOnly = true)
    public AuthTokens login(String username, String password) {
        User user = userRepository.findByUsernameWithAuthorities(username)
            .orElseThrow(() -> new DomainException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    /**
     * Rotate a refresh token: the old jti is consumed (single-use) and a new
     * token family member is issued. Reuse of an already-consumed jti is detected
     * in the store, which revokes the whole family.
     */
    @Transactional(readOnly = true)
    public AuthTokens refresh(String userPublicId, String jti) {
        refreshTokenStore.rotate(userPublicId, jti);
        User user = userRepository.findByPublicIdWithAuthorities(userPublicId)
            .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHENTICATED));
        if (!user.isActive()) {
            refreshTokenStore.revokeAll(userPublicId);
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "Account is inactive");
        }
        return issueTokens(user);
    }

    /** Revoke every refresh token for the user (logout-everywhere). */
    public void logout(String userPublicId) {
        refreshTokenStore.revokeAll(userPublicId);
    }

    private AuthTokens issueTokens(User user) {
        AuthPrincipal principal = toPrincipal(user);
        String jti = randomToken();
        String access = jwtService.issueAccessToken(principal);
        String refresh = jwtService.issueRefreshToken(principal, jti);
        refreshTokenStore.register(user.getPublicId(), jti, jwtService.refreshTtlSeconds());
        String csrf = randomToken();
        return new AuthTokens(
            access, jwtService.accessTtlSeconds(),
            refresh, jwtService.refreshTtlSeconds(),
            csrf, userMapper.toCurrentUser(user));
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String userPublicId) {
        User user = userRepository.findByPublicIdWithAuthorities(userPublicId)
            .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHENTICATED));
        return userMapper.toCurrentUser(user);
    }

    private AuthPrincipal toPrincipal(User user) {
        return new AuthPrincipal(
            user.getPublicId(),
            user.getUsername(),
            new ArrayList<>(user.permissionCodes()));
    }
}
