package com.paypulse.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfigCommon {
    
    @Bean
    public BlacklistChecker blacklistChecker(RedisTemplate<String, String> redisTemplate) {
        return token -> redisTemplate.hasKey("blacklist:" + token);
    }
    
    @Bean
    public JwtAuthFilter jwtAuthFilter(@Value("${JWT_SECRET}") String jwtSecret, BlacklistChecker blacklistChecker) {
        return new JwtAuthFilter(jwtSecret, blacklistChecker);
    }

    
} 