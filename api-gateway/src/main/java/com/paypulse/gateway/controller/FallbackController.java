package com.paypulse.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {
    
    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(ServerWebExchange exchange) {
        log.warn("Auth service fallback triggered for: {}", exchange.getRequest().getURI());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("message", "Auth service temporarily unavailable (circuit breaker)");
        response.put("service", "auth-service");
        
        return Mono.just(ResponseEntity.status(503).body(response));
    }

    @RequestMapping("/fallback/wallet")
    public Mono<ResponseEntity<Map<String, Object>>> walletFallback(ServerWebExchange exchange) {
        log.warn("Wallet service fallback triggered for: {}", exchange.getRequest().getURI());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("message", "Wallet service temporarily unavailable (circuit breaker)");
        response.put("service", "wallet-service");
        
        return Mono.just(ResponseEntity.status(503).body(response));
    }

    @RequestMapping("/fallback/internal-wallet")
    public Mono<ResponseEntity<Map<String, Object>>> internalWalletFallback(ServerWebExchange exchange) {
        log.warn("Internal wallet service fallback triggered for: {}", exchange.getRequest().getURI());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("message", "Internal wallet service temporarily unavailable (circuit breaker)");
        response.put("service", "internal-wallet-service");
        
        return Mono.just(ResponseEntity.status(503).body(response));
    }

    @RequestMapping("/fallback/transaction")
    public Mono<ResponseEntity<Map<String, Object>>> transactionFallback(ServerWebExchange exchange) {
        log.warn("Transaction service fallback triggered for: {}", exchange.getRequest().getURI());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("message", "Transaction service temporarily unavailable (circuit breaker)");
        response.put("service", "transaction-service");
        
        return Mono.just(ResponseEntity.status(503).body(response));
    }

    @RequestMapping("/fallback/notification")
    public Mono<ResponseEntity<Map<String, Object>>> notificationFallback(ServerWebExchange exchange) {
        log.warn("Notification service fallback triggered for: {}", exchange.getRequest().getURI());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("message", "Notification service temporarily unavailable (circuit breaker)");
        response.put("service", "notification-service");
        
        return Mono.just(ResponseEntity.status(503).body(response));
    }

    @RequestMapping("/fallback/analytics")
    public Mono<ResponseEntity<Map<String, Object>>> analyticsFallback(ServerWebExchange exchange) {
        log.warn("Analytics service fallback triggered for: {}", exchange.getRequest().getURI());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("message", "Analytics service temporarily unavailable (circuit breaker)");
        response.put("service", "analytics-service");
        
        return Mono.just(ResponseEntity.status(503).body(response));
    }
} 