package com.paypulse.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final String JWT_SECRET = System.getenv("JWT_SECRET");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String spanId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ") && JWT_SECRET != null) {
            String token = authHeader.substring(7);
            try {
                JwtUtil jwtUtil = new JwtUtil(JWT_SECRET);
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.extractUserId(token).toString();
                    MDC.put("userId", userId);
                }
            } catch (Exception e) {
                log.debug("Failed to parse JWT in LoggingFilter: {}", e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}