package com.springboot.erp.platform.security;

import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Server-side registry of live refresh-token ids (jti), backed by Redis.
 *
 * <p>Refresh tokens are single-use: {@link #register} records a jti when issued,
 * {@link #rotate} atomically consumes it on refresh (and the caller issues a new
 * one). Presenting a signed-but-unknown jti means the token was already used —
 * i.e. a stolen/replayed token — so we {@link #revokeAll} the user's whole token
 * family, forcing re-login. {@link #revokeAll} is also the logout-everywhere and
 * theft-response primitive. This closes the "stolen refresh token valid for 14
 * days" gap that a purely stateless JWT leaves open.
 */
@Component
public class RefreshTokenStore {

    private static final String JTI_PREFIX = "refresh:jti:";
    private static final String USER_PREFIX = "refresh:user:";

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Record a freshly issued refresh jti for the user, expiring with the token. */
    public void register(String userPublicId, String jti, long ttlSeconds) {
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redis.opsForValue().set(JTI_PREFIX + jti, userPublicId, ttl);
        redis.opsForSet().add(USER_PREFIX + userPublicId, jti);
        redis.expire(USER_PREFIX + userPublicId, ttl);
    }

    /**
     * Atomically consume a refresh jti. Returns normally if the jti was live and
     * belongs to the user; throws (after revoking the whole family) if the jti is
     * unknown/already-used — the signature of a replayed or stolen token.
     */
    public void rotate(String userPublicId, String jti) {
        if (jti == null || jti.isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "Invalid refresh token");
        }
        String owner = redis.opsForValue().get(JTI_PREFIX + jti);
        Boolean consumed = redis.delete(JTI_PREFIX + jti);
        boolean live = Boolean.TRUE.equals(consumed) && userPublicId.equals(owner);
        if (!live) {
            // Unknown/replayed jti — treat as compromise: kill every session.
            revokeAll(userPublicId);
            throw new DomainException(ErrorCode.UNAUTHENTICATED, "Refresh token reuse detected");
        }
        redis.opsForSet().remove(USER_PREFIX + userPublicId, jti);
    }

    /** Revoke every refresh token for a user (logout-everywhere / theft response). */
    public void revokeAll(String userPublicId) {
        String userKey = USER_PREFIX + userPublicId;
        Set<String> jtis = redis.opsForSet().members(userKey);
        if (jtis != null) {
            for (String jti : jtis) {
                redis.delete(JTI_PREFIX + jti);
            }
        }
        redis.delete(userKey);
    }
}
