package com.paypulse.auth.dto;

import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "Запрос на вход пользователя")
public class LoginRequest {

    @NotBlank(message = "Username cannot be empty")
    @Schema(description = "Имя пользователя", example = "user1")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Schema(description = "Пароль", example = "Password123!")
    private String password;
}
