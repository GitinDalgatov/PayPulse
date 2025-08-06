package com.paypulse.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class LogResponseBodyFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request);

        long startTime = System.currentTimeMillis();

        log.info("[REQUEST-{}] {} {} from IP: {} - Headers: {}",
                requestId, method, path, clientIp, getHeadersSummary(request.getHeaders()));

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String status = exchange.getResponse().getStatusCode() != null ?
                            exchange.getResponse().getStatusCode().toString() : "UNKNOWN";
                    log.info("[REQUEST-{}] {} {} - Status: {} - Duration: {}ms",
                            requestId, method, path, status, duration);
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

    private String getHeadersSummary(HttpHeaders headers) {
        StringBuilder summary = new StringBuilder("{");
        boolean first = true;

        for (String headerName : headers.keySet()) {
            if (!first) {
                summary.append(", ");
            }
            first = false;


            if ("authorization".equalsIgnoreCase(headerName) ||
                    "cookie".equalsIgnoreCase(headerName)) {
                summary.append(headerName).append(": [REDACTED]");
            } else {
                summary.append(headerName).append(": ").append(headers.getFirst(headerName));
            }
        }

        summary.append("}");
        return summary.toString();
    }

    @Override
    public int getOrder() {
        return 1000;
    }
} 