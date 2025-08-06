package com.paypulse.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final String PREFIX = "refresh:";
    private final RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(String userId, String refreshToken, long ttlMillis) {
        try {
            redisTemplate.opsForValue().set(PREFIX + userId, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
            log.debug("Refresh token saved for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to save refresh token for user: {}", userId, e);
            throw new RuntimeException("Failed to save refresh token", e);
        }
    }

    public String getRefreshToken(String userId) {
        try {
            String token = redisTemplate.opsForValue().get(PREFIX + userId);
            if (token == null) {
                log.debug("No refresh token found for user: {}", userId);
            }
            return token;
        } catch (Exception e) {
            log.error("Failed to get refresh token for user: {}", userId, e);
            throw new RuntimeException("Failed to get refresh token", e);
        }
    }

    public void deleteRefreshToken(String userId) {
        try {
            Boolean deleted = redisTemplate.delete(PREFIX + userId);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Refresh token deleted for user: {}", userId);
            } else {
                log.debug("No refresh token found to delete for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to delete refresh token for user: {}", userId, e);
            throw new RuntimeException("Failed to delete refresh token", e);
        }
    }
}