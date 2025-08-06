package com.paypulse.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/ping",
            "/internal/wallet/**",
            "/monitoring/**",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/docs/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/configuration/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Processing request: {} {}", request.getMethod(), path);


        if (path.startsWith("/docs/") || path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/") ||
                path.startsWith("/swagger-ui.html") || path.startsWith("/webjars/") || path.startsWith("/configuration/")) {
            log.debug("Swagger UI path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }


        if (isPublicPath(path)) {
            log.debug("Public path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }


        if (path.contains("/auth/register") || path.contains("/api/auth/register")) {
            log.debug("Registration path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }


        String token = extractToken(request);
        if (token == null) {
            log.warn("No JWT token found in request: {}", path);
            return unauthorized(exchange, "No JWT token provided");
        }

        Claims claims = validateToken(token);
        if (claims == null) {
            log.warn("Invalid JWT token for request: {}", path);
            return unauthorized(exchange, "Invalid JWT token");
        }

        if (isAdminPath(path) && !hasAdminRole(claims)) {
            log.warn("Insufficient privileges for admin path: {} - user: {}", path, claims.getSubject());
            return forbidden(exchange, "Insufficient privileges");
        }

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-ID", claims.getSubject())
                .header("X-Username", getUsername(claims))
                .header("X-User-Roles", getRoles(claims))
                .header("Authorization", "Bearer " + token)
                .build();

        log.debug("Request authenticated successfully: {} - user: {}", path, claims.getSubject());

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                String basePath = pattern.substring(0, pattern.length() - 2);
                return path.startsWith(basePath);
            }
            return path.equals(pattern) || path.startsWith(pattern + "/");
        });
    }

    private boolean isAdminPath(String path) {
        return path.contains("/admin/") ||
                path.contains("/swagger-ui/") ||
                path.contains("/v3/api-docs/");
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Claims validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Token is null or empty");
                return null;
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return null;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return null;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return null;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasAdminRole(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .anyMatch(role -> role.toString().contains("ADMIN"));
        } else if (roles instanceof String) {
            String rolesStr = (String) roles;
            return rolesStr.contains("ADMIN");
        }
        return false;
    }

    private String getUsername(Claims claims) {
        Object username = claims.get("username");
        return username != null ? username.toString() : "unknown";
    }

    private String getRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return String.join(",", ((List<?>) roles).stream()
                    .map(Object::toString)
                    .toList());
        } else if (roles instanceof String) {
            return (String) roles;
        }
        return "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        String responseBody = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, java.time.LocalDateTime.now());

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        String responseBody = String.format("{\"error\":\"Forbidden\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, java.time.LocalDateTime.now());

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return -100;
    }
} 