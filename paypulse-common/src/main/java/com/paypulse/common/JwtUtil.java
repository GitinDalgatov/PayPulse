package com.paypulse.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtUtil {
    private final String secret;

    public JwtUtil(String secret) {
        this.secret = secret;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration == null || expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        String subject = getAllClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    public List<String> extractRoles(String token) {
        Claims claims = getAllClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> roleList) {
            // Jwt может сериализовать массив ролей как List (например, если roles: [ROLE_USER, ROLE_ADMIN])
            return roleList.stream().map(String::valueOf).toList();
        } else if (roles instanceof String rolesStr) {
            if (rolesStr.isEmpty()) {
                return List.of();
            }
            return List.of(rolesStr.split(","));
        }
        return List.of();
    }

    public String extractUsername(String token) {
        Claims claims = getAllClaims(token);
        Object username = claims.get("username");
        return username != null ? username.toString() : null;
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }
}