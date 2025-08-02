package com.paypulse.analytics.controller;

import com.paypulse.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Analytics API", description = "Аналитика по транзакциям пользователя")
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Получить аналитику пользователя", description = "Возвращает количество и сумму транзакций пользователя")
    @ApiResponse(responseCode = "200", description = "Аналитика получена", content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(value = "{\"transactionCount\": 5, \"totalAmount\": 500.0}")
    ))
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return analyticsService.getUserAnalytics();
    }

    @Operation(summary = "Получить аналитику транзакций", description = "Возвращает список транзакций пользователя")
    @ApiResponse(responseCode = "200", description = "Аналитика транзакций получена")
    @GetMapping("/transactions")
    public List<Map<String, Object>> getTransactions() {
        return analyticsService.getTransactionEvents();
    }

    @Operation(summary = "Получить события баланса", description = "Возвращает список событий изменения баланса")
    @ApiResponse(responseCode = "200", description = "События баланса получены")
    @GetMapping("/balance-events")
    public List<Map<String, Object>> getBalanceEvents() {
        return analyticsService.getBalanceEvents();
    }

    @Operation(summary = "Получить события транзакций", description = "Возвращает список событий транзакций")
    @ApiResponse(responseCode = "200", description = "События транзакций получены")
    @GetMapping("/transaction-events")
    public List<Map<String, Object>> getTransactionEvents() {
        return analyticsService.getTransactionEvents();
    }
}