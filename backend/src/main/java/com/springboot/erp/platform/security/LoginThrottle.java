package com.springboot.erp.platform.security;

import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Brute-force guard for the login endpoint, backed by Redis counters keyed by
 * {@code ip|username}. After {@code maxAttempts} failures inside the rolling
 * window the key is locked for {@code lockMinutes}; a successful login clears it.
 * This throttles password-guessing without adding per-request DB load.
 */
@Component
public class LoginThrottle {

    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redis;
    private final AuthCookieProperties.LoginThrottle cfg;

    public LoginThrottle(StringRedisTemplate redis, AuthCookieProperties props) {
        this.redis = redis;
        this.cfg = props.loginThrottle();
    }

    /** Throw 401 (locked) if this ip+username is currently locked out. */
    public void checkNotLocked(String ip, String username) {
        if (Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + key(ip, username)))) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS,
                "Too many failed attempts. Try again later.");
        }
    }

    /** Record a failed attempt; lock the key once the threshold is crossed. */
    public void recordFailure(String ip, String username) {
        String failKey = FAIL_PREFIX + key(ip, username);
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, Duration.ofMinutes(cfg.windowMinutes()));
        }
        if (count != null && count >= cfg.maxAttempts()) {
            redis.opsForValue().set(LOCK_PREFIX + key(ip, username), "1",
                Duration.ofMinutes(cfg.lockMinutes()));
            redis.delete(failKey);
        }
    }

    /** Clear counters after a successful login. */
    public void reset(String ip, String username) {
        redis.delete(FAIL_PREFIX + key(ip, username));
        redis.delete(LOCK_PREFIX + key(ip, username));
    }

    private String key(String ip, String username) {
        return (ip == null ? "?" : ip) + "|" + (username == null ? "?" : username.toLowerCase());
    }
}
