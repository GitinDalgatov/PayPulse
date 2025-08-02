package com.paypulse.auth.exception;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String username) {
        super("User with username '" + username + "' not found");
    }
    
    public UserNotFoundException(java.util.UUID userId) {
        super("User with ID '" + userId + "' not found");
    }
} 