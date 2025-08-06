package com.paypulse.auth.exception;

public class UserAlreadyExistsException extends AuthException {
    public UserAlreadyExistsException(String username) {
        super("User with username '" + username + "' already exists");
    }
} 