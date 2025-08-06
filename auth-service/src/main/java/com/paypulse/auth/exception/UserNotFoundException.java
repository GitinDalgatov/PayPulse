package com.paypulse.auth.exception;

import java.util.UUID;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String username) {
        super("User with username '" + username + "' not found");
    }

    public UserNotFoundException(UUID userId) {
        super("User with ID '" + userId + "' not found");
    }
} 