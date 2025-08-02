
package com.paypulse.auth.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "Запрос на обновление токена")
public class RefreshRequest {

    @NotBlank(message = "Refresh token cannot be empty")
    @Schema(description = "Refresh токен", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
