package com.paypulse.transaction.controller;

import com.paypulse.common.TransactionRequest;
import com.paypulse.common.TransactionResponse;
import com.paypulse.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import io.swagger.v3.oas.annotations.*;

@Tag(name = "Transaction API", description = "Операции с транзакциями между пользователями")
@RestController
@RequestMapping({"/api/transactions", "/transactions"})
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @Operation(
            summary = "Создать транзакцию",
            description = "Создает новую транзакцию между пользователями"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Транзакция создана",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для создания транзакции",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TransactionRequest.class),
                            examples = @ExampleObject(value = "{\"fromUserId\":\"uuid1\",\"toUserId\":\"uuid2\",\"amount\":100.0}")
                    )
            )
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @Operation(
            summary = "История транзакций пользователя",
            description = "Возвращает историю транзакций пользователя"
    )
    @ApiResponse(
            responseCode = "200",
            description = "История получена",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))
    )
    @GetMapping({"/", "/history", ""})
    public ResponseEntity<List<TransactionResponse>> getHistory() {
        return ResponseEntity.ok(service.getHistory());
    }

    @Operation(
            summary = "Метрики outbox",
            description = "Возвращает метрики outbox паттерна"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Метрики получены"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/outbox-metrics")
    public ResponseEntity<Map<String, Object>> getOutboxMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pendingMessages", 0);
        metrics.put("processedMessages", 0);
        metrics.put("failedMessages", 0);
        metrics.put("lastProcessedAt", null);
        return ResponseEntity.ok(metrics);
    }
}