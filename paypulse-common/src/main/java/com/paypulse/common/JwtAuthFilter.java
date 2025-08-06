package com.paypulse.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BlacklistChecker blacklistChecker;

    public JwtAuthFilter(String secret, BlacklistChecker blacklistChecker) {
        this.jwtUtil = new JwtUtil(secret);
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Проверка blacklisted токена
            if (blacklistChecker != null && blacklistChecker.isBlacklisted(token)) {
                log.warn("Token is blacklisted");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write("Token is blacklisted");
                return;
            }

            // Проверка JWT-токена
            if (jwtUtil.validateToken(token)) {
                UUID userId = jwtUtil.extractUserId(token);
                List<String> roles = jwtUtil.extractRoles(token);
                log.debug("Extracted roles for user {}: {}", userId, roles);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                        .toList();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId.toString(), token, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authentication set for user: {}", userId);
            }
        }
        chain.doFilter(req, res);
    }
}