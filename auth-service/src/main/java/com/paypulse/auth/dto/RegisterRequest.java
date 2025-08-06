package com.paypulse.auth.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;

@Schema(description = "Запрос на регистрацию пользователя")
public record RegisterRequest(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
        @Schema(description = "Имя пользователя", example = "user1")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Schema(description = "Пароль", example = "Password123!")
        String password,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email should be valid")
        @Schema(description = "Email пользователя", example = "user@example.com")
        String email,

        @Schema(description = "Роли", example = "[\"ROLE_USER\"]")
        Set<String> roles
) {
}