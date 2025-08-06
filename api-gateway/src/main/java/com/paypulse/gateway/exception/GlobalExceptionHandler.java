package com.paypulse.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse;
        HttpStatus status;

        if (ex instanceof WebExchangeBindException bindEx) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "path", exchange.getRequest().getURI().getPath(),
                    "method", exchange.getRequest().getMethod().name(),
                    "status", status.value(),
                    "error", "Validation Error",
                    "message", "Validation failed for request",
                    "fieldErrors", bindEx.getFieldErrors().stream()
                            .collect(Collectors.toMap(
                                    error -> error.getField(),
                                    error -> error.getDefaultMessage()
                            ))
            );
        } else if (ex instanceof ServerWebInputException inputEx) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "path", exchange.getRequest().getURI().getPath(),
                    "method", exchange.getRequest().getMethod().name(),
                    "status", status.value(),
                    "error", "Invalid Input",
                    "message", inputEx.getMessage()
            );
        } else if (ex instanceof ResponseStatusException rse) {
            // Безопасно преобразуем HttpStatusCode -> HttpStatus (Spring 6+)
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "path", exchange.getRequest().getURI().getPath(),
                    "method", exchange.getRequest().getMethod().name(),
                    "status", rse.getStatusCode().value(),
                    "error", "Request Failed",
                    "message", rse.getReason() != null ? rse.getReason() : rse.getMessage()
            );
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "path", exchange.getRequest().getURI().getPath(),
                    "method", exchange.getRequest().getMethod().name(),
                    "status", status.value(),
                    "error", "Internal Server Error",
                    "message", "Произошла внутренняя ошибка сервера"
            );
        }

        response.setStatusCode(status);

        if (status.is5xxServerError()) {
            log.error("[EXCEPTION] Error processing request: {} - {}", exchange.getRequest().getURI(), ex.getMessage(), ex);
        } else {
            log.warn("[EXCEPTION] {}: {} - {}", status, exchange.getRequest().getURI(), ex.getMessage());
        }

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to serialize error response", e);
            String fallbackResponse = "{\"error\":\"Internal Server Error\",\"message\":\"Failed to serialize error response\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallbackResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }
}