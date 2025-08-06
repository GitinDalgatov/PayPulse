package com.paypulse.notification.controller;

import com.paypulse.common.NotificationResponse;
import com.paypulse.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification API", description = "Уведомления пользователя")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @Operation(
            summary = "Получить уведомления пользователя",
            description = "Возвращает список уведомлений пользователя"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Уведомления получены",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))
    )
    @GetMapping
    public List<NotificationResponse> getNotifications() {
        return service.getNotifications();
    }
}