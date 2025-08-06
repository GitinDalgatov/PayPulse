package com.paypulse.auth.controller;

import com.paypulse.auth.dto.AuthResponse;
import com.paypulse.auth.dto.LoginRequest;
import com.paypulse.auth.dto.RegisterRequest;
import com.paypulse.auth.dto.RefreshRequest;
import com.paypulse.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Slf4j
@Tag(name = "Auth API", description = "Аутентификация и управление пользователями")
@RestController
@RequestMapping({"/api/auth", "/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Регистрация пользователя", description = "Создает нового пользователя и возвращает JWT токены")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для регистрации",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(value = "{\"username\":\"user1\",\"password\":\"Password123!\",\"roles\":[\"ROLE_USER\"]}")
                    )
            )
            @Valid @RequestBody RegisterRequest req) {
        log.info("Received registration request for user: {}", req.username());
        return authService.register(req);
    }

    @Operation(summary = "Логин пользователя", description = "Аутентификация пользователя и выдача JWT токенов")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    @PostMapping("/login")
    public AuthResponse login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для входа",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = "{\"username\":\"user1\",\"password\":\"Password123!\"}")
                    )
            )
            @Valid @RequestBody LoginRequest req) {
        log.info("Received login request for user: {}", req.username());
        return authService.login(req);
    }

    @Operation(summary = "Обновление токена", description = "Обновляет access и refresh токены по refresh токену")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены обновлены", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Неверный refresh токен")
    })
    @PostMapping("/refresh")
    public AuthResponse refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh токен",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshRequest.class),
                            examples = @ExampleObject(value = "{\"refreshToken\":\"...\"}")
                    )
            )
            @Valid @RequestBody RefreshRequest req) {
        log.info("Received token refresh request");
        return authService.refresh(req);
    }

    @Operation(summary = "Назначение роли пользователю", description = "Только для ADMIN. Назначает роль пользователю.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль назначена"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public void assignRole(
            @Parameter(description = "Имя пользователя", example = "user1") @RequestParam String username,
            @Parameter(description = "Роль", example = "ROLE_ADMIN") @RequestParam String role) {
        log.info("Received role assignment request: {} -> {}", username, role);
        authService.assignRole(username, role);
    }

    @Operation(summary = "Выход пользователя", description = "Logout: access-токен добавляется в blacklist, refresh-токен удаляется из Redis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Выход выполнен успешно"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Неверный токен")
    })
    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authHeader) {
        log.info("Received logout request");
        authService.logout(authHeader);
    }

    @Operation(summary = "Health check", description = "Проверка состояния сервиса")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сервис работает")
    })
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}