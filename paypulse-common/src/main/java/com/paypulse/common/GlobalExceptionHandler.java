package com.paypulse.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldError() != null ?
            ex.getBindingResult().getFieldError().getDefaultMessage() : "Validation failed";
        return buildError(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
    }
    

    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex, HttpServletRequest req) {
        String className = ex.getClass().getName();
        
        
        if (className.equals("com.paypulse.auth.exception.UserAlreadyExistsException")) {
            return buildError(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
        }
        if (className.equals("com.paypulse.auth.exception.InvalidCredentialsException")) {
            return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI());
        }
        if (className.equals("com.paypulse.auth.exception.InvalidTokenException")) {
            return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI());
        }
        if (className.equals("com.paypulse.auth.exception.UserNotFoundException")) {
            return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
        }
        
        log.error("Internal error", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", req.getRequestURI());
    }
    private ResponseEntity<?> buildError(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
} 