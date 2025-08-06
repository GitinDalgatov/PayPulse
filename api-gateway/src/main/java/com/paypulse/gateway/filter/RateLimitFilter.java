package com.paypulse.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {


    @Value("${rate-limit.max-requests-per-minute:100}")
    private int maxRequestsPerMinute;

    @Value("${rate-limit.window-size-minutes:1}")
    private int windowSizeMinutes;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(request);
        String path = request.getURI().getPath();


        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        String key = "rate_limit:" + clientIp + ":" + path;
        String windowKey = "rate_limit_window:" + clientIp + ":" + path;

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {

                        return redisTemplate.expire(key, Duration.ofMinutes(windowSizeMinutes))
                                .then(Mono.just(count));
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > maxRequestsPerMinute) {
                        log.warn("Rate limit exceeded for IP: {}, path: {}, count: {}", clientIp, path, count);
                        return handleRateLimitExceeded(exchange);
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    log.error("Rate limiting error", e);
                    return chain.filter(exchange);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator/") ||
                path.startsWith("/monitoring/") ||
                path.equals("/health") ||
                path.equals("/info");
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("X-Rate-Limit-Reset",
                String.valueOf(System.currentTimeMillis() + (windowSizeMinutes * 60 * 1000)));

        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
} 