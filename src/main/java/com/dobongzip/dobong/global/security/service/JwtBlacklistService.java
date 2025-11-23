package com.dobongzip.dobong.global.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {
    private final StringRedisTemplate redis;
    private String key(String token) { return "jwt:blacklist:" + token.trim(); } // ★ trim

    /** 남은 만료시간 동안만 블락 (최소 1초 보정) */
    public boolean block(String token, Duration ttl) {
        if (token == null || token.isBlank()) return false;
        // ✔ 최소 TTL 보정: null, 0, 음수 → 1초로
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofSeconds(1);
        }
        String k = "jwt:blacklist:" + token.trim();
        redis.opsForValue().set(k, "1", ttl);
        System.out.println("[BLACKLIST] SET key=" + k + " ttl=" + ttl.getSeconds() + "s");
        return true;
    }

    public boolean isBlocked(String token) {
        if (token == null || token.isBlank()) return false;
        Boolean exists = redis.hasKey(key(token));
        return exists != null && exists;
    }
}
