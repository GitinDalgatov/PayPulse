
package com.paypulse.auth.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final Key key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiration-ms}") long accessExp,
        @Value("${jwt.refresh-expiration-ms}") long refreshExp
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMs = accessExp;
        this.refreshTokenExpirationMs = refreshExp;
        log.info("JwtService initialized with access token expiration: {}ms, refresh token expiration: {}ms", 
                accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    public String generateAccessToken(UUID userId, String username, String roles) {
        try {
            String token = Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(key)
                .compact();
            
            log.debug("Generated access token for user: {}", username);
            return token;
        } catch (Exception e) {
            log.error("Error generating access token for user: {}", username, e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public String generateRefreshToken(UUID userId, String username, String roles) {
        try {
            String token = Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(key)
                .compact();
            
            log.debug("Generated refresh token for user: {}", username);
            return token;
        } catch (Exception e) {
            log.error("Error generating refresh token for user: {}", username, e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
            
            String tokenType = claims.get("type", String.class);
            if (tokenType == null) {
                log.warn("Token missing type claim");
                return false;
            }
            
            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                log.warn("Token expired");
                return false;
            }
            
            log.debug("Token validation successful for user: {}", claims.get("username"));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
            
            return claims.get("username", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    public UUID extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
            
            String subject = claims.getSubject();
            return UUID.fromString(subject);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            throw new RuntimeException("Failed to extract user ID from token", e);
        }
    }

    public String extractRoles(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
            
            String roles = claims.get("roles", String.class);
            return roles != null ? roles : "";
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract roles from token: {}", e.getMessage());
            return "";
        }
    }

    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();
            
            return claims.get("type", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return null;
        }
    }
}
