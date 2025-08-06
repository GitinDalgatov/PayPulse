package com.paypulse.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Slf4j
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(
            @PathVariable String service,
            ServerWebExchange exchange) {

        ServiceInfo info = getServiceInfo(service);

        log.warn("{} fallback triggered for: {}", info.name(), exchange.getRequest().getURI());

        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 503,
                "error", "Service Unavailable",
                "message", info.message(),
                "service", info.name()
        );

        return Mono.just(ResponseEntity.status(503).body(response));
    }

    private static ServiceInfo getServiceInfo(String service) {
        return switch (service) {
            case "auth" -> new ServiceInfo("auth-service", "Auth service temporarily unavailable (circuit breaker)");
            case "wallet" -> new ServiceInfo("wallet-service", "Wallet service temporarily unavailable (circuit breaker)");
            case "internal-wallet" -> new ServiceInfo("internal-wallet-service", "Internal wallet service temporarily unavailable (circuit breaker)");
            case "transaction" -> new ServiceInfo("transaction-service", "Transaction service temporarily unavailable (circuit breaker)");
            case "notification" -> new ServiceInfo("notification-service", "Notification service temporarily unavailable (circuit breaker)");
            case "analytics" -> new ServiceInfo("analytics-service", "Analytics service temporarily unavailable (circuit breaker)");
            default -> new ServiceInfo(service + "-service", "Service temporarily unavailable (circuit breaker)");
        };
    }

    private record ServiceInfo(String name, String message) {}
}