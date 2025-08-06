package com.paypulse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на обновление токена")
public record RefreshRequest(
        @NotBlank(message = "Refresh token cannot be empty")
        @Schema(description = "Refresh токен", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken
) {}