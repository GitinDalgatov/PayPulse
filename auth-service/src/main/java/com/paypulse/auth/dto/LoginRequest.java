package com.paypulse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на вход пользователя")
public record LoginRequest(
        @NotBlank(message = "Username cannot be empty")
        @Schema(description = "Имя пользователя", example = "user1")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Schema(description = "Пароль", example = "Password123!")
        String password
) {}