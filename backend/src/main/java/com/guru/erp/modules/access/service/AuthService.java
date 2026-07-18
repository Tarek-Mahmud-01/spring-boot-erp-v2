package com.guru.erp.modules.access.service;

import com.guru.erp.modules.access.domain.User;
import com.guru.erp.modules.access.dto.AuthDtos.CurrentUserResponse;
import com.guru.erp.modules.access.dto.AuthDtos.LoginResponse;
import com.guru.erp.modules.access.mapper.UserMapper;
import com.guru.erp.modules.access.repository.UserRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.AuthPrincipal;
import com.guru.erp.platform.security.JwtService;
import java.util.ArrayList;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsernameWithAuthorities(username)
            .orElseThrow(() -> new DomainException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }
        AuthPrincipal principal = toPrincipal(user);
        String access = jwtService.issueAccessToken(principal);
        String refresh = jwtService.issueRefreshToken(principal);
        return new LoginResponse(access, refresh, jwtService.accessTtlSeconds(), userMapper.toCurrentUser(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String userPublicId) {
        User user = userRepository.findByPublicIdWithAuthorities(userPublicId)
            .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHENTICATED));
        if (!user.isActive()) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "Account is inactive");
        }
        AuthPrincipal principal = toPrincipal(user);
        String access = jwtService.issueAccessToken(principal);
        String refresh = jwtService.issueRefreshToken(principal);
        return new LoginResponse(access, refresh, jwtService.accessTtlSeconds(), userMapper.toCurrentUser(user));
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
