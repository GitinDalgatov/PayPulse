
package com.paypulse.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@Schema(description = "Ответ с JWT токенами")
public class AuthResponse {
    @Schema(description = "Access токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private String accessToken;
    @Schema(description = "Refresh токен", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private String refreshToken;
}
