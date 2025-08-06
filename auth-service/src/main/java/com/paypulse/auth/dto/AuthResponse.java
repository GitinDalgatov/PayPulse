package com.paypulse.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с JWT токенами")
public record AuthResponse(
        @Schema(description = "Access токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
        String accessToken,
        @Schema(description = "Refresh токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
        String refreshToken
) {}