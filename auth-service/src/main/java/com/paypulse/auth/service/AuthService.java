
package com.paypulse.auth.service;

import com.paypulse.auth.config.JwtService;
import com.paypulse.auth.dto.AuthResponse;
import com.paypulse.auth.dto.LoginRequest;
import com.paypulse.auth.dto.RefreshRequest;
import com.paypulse.auth.dto.RegisterRequest;
import com.paypulse.auth.entity.User;
import com.paypulse.auth.exception.*;
import com.paypulse.auth.repository.UserRepository;
import com.paypulse.auth.validation.PasswordValidator;
import com.paypulse.auth.validation.UsernameValidator;
import com.paypulse.common.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenBlacklistService accessTokenBlacklistService;
    private final PasswordValidator passwordValidator;
    private final UsernameValidator usernameValidator;
    private final AuditService auditService;

    //TODO
    @Value("${jwt.access-expiration-ms}")
    private long accessTokenTtl;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        log.info("Processing registration request for user: {}", req.username());

        try {

            if (req.username() == null || req.username().trim().isEmpty()) {
                log.warn("Username is null or empty");
                throw new IllegalArgumentException("Username cannot be null or empty");
            }

            if (req.password() == null || req.password().trim().isEmpty()) {
                log.warn("Password is null or empty for user: {}", req.username());
                throw new IllegalArgumentException("Password cannot be null or empty");
            }


            UsernameValidator.ValidationResult usernameValidation = usernameValidator.validateUsername(req.username());
            if (!usernameValidation.valid()) {
                log.warn("Username validation failed for user: {}", req.username());
                throw new IllegalArgumentException("Username validation failed: " + String.join(", ", usernameValidation.errors()));
            }


            PasswordValidator.ValidationResult passwordValidation = passwordValidator.validatePassword(req.password());
            if (!passwordValidation.valid()) {
                log.warn("Password validation failed for user: {}", req.username());
                throw new IllegalArgumentException("Password validation failed: " + String.join(", ", passwordValidation.errors()));
            }


            userRepository.findByUsername(req.username())
                    .ifPresent(u -> {
                        log.warn("User already exists: {}", req.username());
                        throw new UserAlreadyExistsException(req.username());
                    });

            String roles = (req.roles() != null && !req.roles().isEmpty()) ? String.join(",", req.roles()) : "ROLE_USER";

            User user = User.builder()
                    .userId(UUID.randomUUID())
                    .username(req.username())
                    .password(passwordEncoder.encode(req.password()))
                    .email(req.email())
                    .roles(roles)
                    .build();

            String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getUsername(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken(user.getUserId(), user.getUsername(), user.getRoles());

            userRepository.save(user);
            refreshTokenService.saveRefreshToken(user.getUserId().toString(), refreshToken, 7 * 24 * 60 * 60 * 1000L);

            auditService.logAction(user.getUsername(), "register", "roles=" + roles);
            log.info("User registered successfully: {}", user.getUsername());

            return new AuthResponse(accessToken, refreshToken);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during user registration for user: {}", req.username(), e);
            throw new RuntimeException("Registration failed", e);
        }
    }

    public AuthResponse login(LoginRequest req) {
        log.info("Processing login request for user: {}", req.username());

        try {
            User user = userRepository.findByUsername(req.username())
                    .orElseThrow(() -> {
                        log.warn("Login attempt with non-existent username: {}", req.username());
                        return new InvalidCredentialsException();
                    });

            if (!passwordEncoder.matches(req.password(), user.getPassword())) {
                log.warn("Login attempt with wrong password for user: {}", req.username());
                throw new InvalidCredentialsException();
            }

            String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getUsername(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken(user.getUserId(), user.getUsername(), user.getRoles());


            refreshTokenService.saveRefreshToken(user.getUserId().toString(), refreshToken, 7 * 24 * 60 * 60 * 1000L);

            auditService.logAction(user.getUsername(), "login", "success");
            log.info("User logged in successfully: {}", user.getUsername());

            return new AuthResponse(accessToken, refreshToken);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during login for user: {}", req.username(), e);
            throw new RuntimeException("Login failed", e);
        }
    }

    public AuthResponse refresh(RefreshRequest req) {
        log.info("Processing token refresh request");

        try {

            final UUID userId;
            try {
                userId = jwtService.extractUserId(req.refreshToken());
            } catch (Exception e) {
                log.warn("Invalid refresh token format");
                throw new InvalidTokenException("Invalid refresh token format");
            }

            String storedRefreshToken = refreshTokenService.getRefreshToken(userId.toString());
            if (storedRefreshToken == null || !storedRefreshToken.equals(req.refreshToken())) {
                log.warn("Refresh token mismatch for user: {}", userId);
                throw new InvalidTokenException("Invalid refresh token");
            }

            if (!jwtService.validateToken(req.refreshToken())) {
                log.warn("Invalid refresh token validation for user: {}", userId);
                throw new InvalidTokenException("Invalid refresh token");
            }

            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getUsername(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken(user.getUserId(), user.getUsername(), user.getRoles());


            refreshTokenService.saveRefreshToken(user.getUserId().toString(), refreshToken, 7 * 24 * 60 * 60 * 1000L);

            auditService.logAction(user.getUsername(), "refresh", "token refreshed");
            log.info("Token refreshed successfully for user: {}", user.getUsername());

            return new AuthResponse(accessToken, refreshToken);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    @Transactional
    public void assignRole(String username, String role) {
        log.info("Assigning role '{}' to user: {}", role, username);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException(username));

            String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            String currentRoles = user.getRoles();
            String newRoles = currentRoles.isEmpty() ? normalizedRole : currentRoles + "," + normalizedRole;
            user.setRoles(newRoles);
            userRepository.save(user);

            auditService.logAction(username, "assignRole", normalizedRole);
            log.info("Role '{}' assigned successfully to user: {}", normalizedRole, username);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error assigning role '{}' to user: {}", role, username, e);
            throw new RuntimeException("Role assignment failed", e);
        }
    }

    public void logout(String authHeader) {
        log.info("Processing logout request");

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No valid authorization header provided for logout");
                throw new InvalidTokenException("No token provided");
            }

            String token = authHeader.substring(7);


            final java.util.UUID userId;
            try {
                userId = jwtService.extractUserId(token);
            } catch (Exception e) {
                log.warn("Invalid token format during logout");
                throw new InvalidTokenException("Invalid token");
            }


            accessTokenBlacklistService.blacklistToken(token, accessTokenTtl);


            refreshTokenService.deleteRefreshToken(userId.toString());

            log.info("User logged out successfully: {}", userId);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new RuntimeException("Logout failed", e);
        }
    }
}
