package com.springboot.erp.platform.security;

import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
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

    /** The built-in dev secret; forging tokens is trivial if this ships to prod. */
    private static final String INSECURE_DEV_SECRET =
        "dev-only-change-me-32-bytes-minimum-secret-key";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props, AuthCookieProperties authProps) {
        this.props = props;
        byte[] secretBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 bytes for HS256 (set JWT_SECRET)");
        }
        // Fail fast: never run with the public dev secret unless explicitly allowed.
        // A shipped default secret means anyone can mint a valid admin token.
        if (INSECURE_DEV_SECRET.equals(props.secret()) && !authProps.allowInsecureSecret()) {
            throw new IllegalStateException(
                "Refusing to start with the built-in dev JWT secret. Set a strong JWT_SECRET "
                    + "(>=32 bytes), or set AUTH_ALLOW_INSECURE_SECRET=true for local dev only.");
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

    /**
     * Issue a refresh token carrying a unique {@code jti}. The jti is registered
     * in the {@link RefreshTokenStore} so the token can be rotated (single-use)
     * and revoked server-side — a signed-but-unknown jti means reuse/theft.
     */
    public String issueRefreshToken(AuthPrincipal principal, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.issuer())
            .id(jti)
            .subject(principal.userPublicId())
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(now.plus(Duration.ofDays(props.refreshTtlDays()))))
            .signWith(key)
            .compact();
    }

    public long refreshTtlSeconds() {
        return Duration.ofDays(props.refreshTtlDays()).toSeconds();
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

    /** A verified refresh token's identity: which user, and which single-use jti. */
    public record RefreshClaims(String userPublicId, String jti) {
    }

    /** Parse & verify a refresh token's signature/issuer/type, returning its claims. */
    public RefreshClaims parseRefreshToken(String token) {
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
            return new RefreshClaims(claims.getSubject(), claims.getId());
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
