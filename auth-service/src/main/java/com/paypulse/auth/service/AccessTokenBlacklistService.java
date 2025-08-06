package com.paypulse.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistService {
    private static final String PREFIX = "blacklist:";
    private final RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(String token, long ttlMillis) {
        try {
            redisTemplate.opsForValue().set(PREFIX + token, "1", ttlMillis, TimeUnit.MILLISECONDS);
            log.debug("Token blacklisted: {}", token.substring(0, Math.min(token.length(), 10)));
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = redisTemplate.hasKey(PREFIX + token);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check if token is blacklisted", e);
            return false;
        }
    }
}