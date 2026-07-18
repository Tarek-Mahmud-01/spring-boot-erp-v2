package com.guru.erp.platform.security;

import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Stateless JWT issue/verify (ARCHITECTURE.md §2 — stateless JWT). Access
 * tokens embed the user's ULID, username, and flattened permission codes so
 * authorization needs no DB lookup per request.
 */
@Service
public class JwtService {

    private static final String CLAIM_PERMISSIONS = "perms";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] secretBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 bytes for HS256 (set JWT_SECRET)");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String issueAccessToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.issuer())
            .subject(principal.userPublicId())
            .claim(CLAIM_USERNAME, principal.username())
            .claim(CLAIM_PERMISSIONS, principal.permissions())
            .claim(CLAIM_TYPE, TYPE_ACCESS)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(now.plus(Duration.ofMinutes(props.accessTtlMinutes()))))
            .signWith(key)
            .compact();
    }

    public String issueRefreshToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.issuer())
            .subject(principal.userPublicId())
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(now.plus(Duration.ofDays(props.refreshTtlDays()))))
            .signWith(key)
            .compact();
    }

    /** Parse & verify an access token into a principal, or throw a DomainException. */
    @SuppressWarnings("unchecked")
    public AuthPrincipal parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new DomainException(ErrorCode.UNAUTHENTICATED, "Not an access token");
            }
            List<String> perms = claims.get(CLAIM_PERMISSIONS, List.class);
            return new AuthPrincipal(
                claims.getSubject(),
                claims.get(CLAIM_USERNAME, String.class),
                perms == null ? List.of() : List.copyOf(perms));
        } catch (ExpiredJwtException e) {
            throw new DomainException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "Invalid token");
        }
    }

    /** Parse & verify a refresh token, returning the subject (user public id). */
    public String parseRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new DomainException(ErrorCode.UNAUTHENTICATED, "Not a refresh token");
            }
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            throw new DomainException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "Invalid refresh token");
        }
    }

    public long accessTtlSeconds() {
        return Duration.ofMinutes(props.accessTtlMinutes()).toSeconds();
    }
}
