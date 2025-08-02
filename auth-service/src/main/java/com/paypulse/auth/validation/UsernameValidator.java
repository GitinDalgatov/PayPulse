package com.paypulse.auth.validation;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class UsernameValidator {
    
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    public ValidationResult validateUsername(String username) {
        List<String> errors = new ArrayList<>();
        
        if (username == null || username.trim().isEmpty()) {
            errors.add("Username cannot be empty");
            return new ValidationResult(false, errors);
        }
        
        String trimmedUsername = username.trim();
        
        if (trimmedUsername.length() < MIN_LENGTH) {
            errors.add("Username must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (trimmedUsername.length() > MAX_LENGTH) {
            errors.add("Username must not exceed " + MAX_LENGTH + " characters");
        }
        
        if (!USERNAME_PATTERN.matcher(trimmedUsername).matches()) {
            errors.add("Username can only contain letters, numbers, underscores and hyphens");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
    }
} 