package com.paypulse.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public final class JwtService {

    private final Key key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExp,
            @Value("${jwt.refresh-expiration-ms}") long refreshExp
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 chars for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMs = accessExp;
        this.refreshTokenExpirationMs = refreshExp;
        log.info("JwtService initialized with access token expiration: {}ms, refresh token expiration: {}ms",
                accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    public String generateAccessToken(UUID userId, String username, String roles) {
        try {
            return Jwts.builder()
                    .setSubject(userId.toString())
                    .claim("username", username)
                    .claim("roles", roles)
                    .claim("type", "access")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating access token for user: {}", username, e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public String generateRefreshToken(UUID userId, String username, String roles) {
        try {
            return Jwts.builder()
                    .setSubject(userId.toString())
                    .claim("username", username)
                    .claim("roles", roles)
                    .claim("type", "refresh")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating refresh token for user: {}", username, e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get("type", String.class);
            if (tokenType == null) {
                log.warn("Token missing type claim");
                return false;
            }
            log.debug("Token validated: {}", claims.get("username"));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return parseClaims(token).get("username", String.class);
        } catch (Exception e) {
            log.warn("Failed to extract username: {}", e.getMessage());
            return null;
        }
    }

    public UUID extractUserId(String token) {
        try {
            String subject = parseClaims(token).getSubject();
            return UUID.fromString(subject);
        } catch (Exception e) {
            log.warn("Failed to extract userId: {}", e.getMessage());
            throw new RuntimeException("Failed to extract userId from token", e);
        }
    }

    public String extractRoles(String token) {
        try {
            String roles = parseClaims(token).get("roles", String.class);
            return roles != null ? roles : "";
        } catch (Exception e) {
            log.warn("Failed to extract roles: {}", e.getMessage());
            return "";
        }
    }

    public String getTokenType(String token) {
        try {
            return parseClaims(token).get("type", String.class);
        } catch (Exception e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}