package com.paypulse.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        return buildError(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex, HttpServletRequest req) {
        String className = ex.getClass().getName();
        HttpStatus status;

        switch (className) {
            case "com.paypulse.auth.exception.UserAlreadyExistsException":
                status = HttpStatus.CONFLICT; break;
            case "com.paypulse.auth.exception.InvalidCredentialsException":
            case "com.paypulse.auth.exception.InvalidTokenException":
                status = HttpStatus.UNAUTHORIZED; break;
            case "com.paypulse.auth.exception.UserNotFoundException":
                status = HttpStatus.NOT_FOUND; break;
            default:
                log.error("Internal error", ex);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String msg = ex.getMessage() != null ? ex.getMessage() : "Internal error";
        return buildError(status, msg, req.getRequestURI());
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