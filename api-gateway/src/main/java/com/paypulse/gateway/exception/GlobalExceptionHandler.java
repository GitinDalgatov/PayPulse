package com.paypulse.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", exchange.getRequest().getURI().getPath());
        errorResponse.put("method", exchange.getRequest().getMethod().name());
        
        log.error("[EXCEPTION] Error processing request: {} - {}", exchange.getRequest().getURI(), ex.getMessage(), ex);
        
        if (ex instanceof WebExchangeBindException) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Validation Error");
            errorResponse.put("message", "Validation failed for request");
            
            Map<String, String> fieldErrors = new HashMap<>();
            ((WebExchangeBindException) ex).getBindingResult().getFieldErrors().forEach(error -> 
                fieldErrors.put(error.getField(), error.getDefaultMessage())
            );
            errorResponse.put("fieldErrors", fieldErrors);
            
        } else if (ex instanceof ServerWebInputException) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Invalid Input");
            errorResponse.put("message", ex.getMessage());
            
        } else if (ex instanceof org.springframework.web.server.ResponseStatusException) {
            org.springframework.web.server.ResponseStatusException rse = 
                (org.springframework.web.server.ResponseStatusException) ex;
            response.setStatusCode(rse.getStatusCode());
            errorResponse.put("status", rse.getStatusCode().value());
            errorResponse.put("error", "Request Failed");
            errorResponse.put("message", rse.getReason());
            
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "Произошла внутренняя ошибка сервера");
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