package com.paypulse.wallet.controller;

import com.paypulse.common.BalanceResponse;
import com.paypulse.common.DepositRequest;
import com.paypulse.common.HistoryResponse;
import com.paypulse.common.WithdrawRequest;
import com.paypulse.wallet.service.OutboxProcessor;
import com.paypulse.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Wallet API", description = "Операции с кошельком пользователя")
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final OutboxProcessor outboxProcessor;

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        if (!isValidSortField(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }
        if (!isValidSortDirection(sortDir)) {
            throw new IllegalArgumentException("Invalid sort direction: " + sortDir);
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    private boolean isValidSortField(String sortBy) {
        return sortBy != null && switch (sortBy) {
            case "timestamp", "amount", "description" -> true;
            default -> false;
        };
    }

    private boolean isValidSortDirection(String sortDir) {
        return sortDir != null && ("asc".equalsIgnoreCase(sortDir) || "desc".equalsIgnoreCase(sortDir));
    }

    @Operation(summary = "Получить баланс пользователя", description = "Возвращает текущий баланс пользователя")
    @ApiResponse(responseCode = "200", description = "Баланс получен", content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance() {
        return ResponseEntity.ok(walletService.getBalance());
    }

    @Operation(summary = "Пополнить баланс", description = "Пополняет баланс пользователя на указанную сумму")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс пополнен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    })
    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для пополнения",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = DepositRequest.class),
                            examples = @ExampleObject(value = "{\"amount\":100.0,\"description\":\"Пополнение\"}")
                    )
            )
            @Valid @RequestBody DepositRequest request) {
        walletService.deposit(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Списать средства с кошелька", description = "Проверяет баланс, списывает сумму, защищено от гонок")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Списание успешно"),
            @ApiResponse(responseCode = "400", description = "Недостаточно средств")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return walletService.withdraw(request)
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }

    @Operation(summary = "История операций пользователя", description = "Возвращает историю операций пользователя с пагинацией")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "История получена", content = @Content(schema = @Schema(implementation = HistoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации параметров")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<HistoryResponse>> getHistory(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Сортировка (timestamp, amount, description)", example = "timestamp") @RequestParam(defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Направление сортировки (asc, desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(walletService.getHistory(pageable));
    }

    @Operation(summary = "Вся история операций (ADMIN)", description = "Возвращает всю историю операций с пагинацией (только для ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Вся история получена", content = @Content(schema = @Schema(implementation = HistoryResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @GetMapping("/all-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<HistoryResponse>> getAllHistory(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Сортировка (timestamp, amount, description)", example = "timestamp") @RequestParam(defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Направление сортировки (asc, desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(walletService.getAllHistory(pageable));
    }

    @Operation(summary = "Метрики outbox", description = "Возвращает метрики outbox паттерна")
    @ApiResponse(responseCode = "200", description = "Метрики получены")
    @GetMapping("/outbox-metrics")
    public ResponseEntity<OutboxProcessor.OutboxMetrics> getOutboxMetrics() {
        return ResponseEntity.ok(outboxProcessor.getMetrics());
    }

    @Operation(summary = "Админское пополнение баланса", description = "Пополняет баланс пользователя по ID (только для ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс пополнен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @PostMapping("/admin/deposit/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeposit(
            @Parameter(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID userId,
            @Valid @RequestBody DepositRequest request) {
        walletService.adminDeposit(userId.toString(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Внутреннее пополнение баланса", description = "Пополняет баланс пользователя по ID (для внутренних вызовов)")
    @ApiResponse(responseCode = "200", description = "Баланс пополнен")
    @PostMapping("/internal/deposit/{userId}")
    public ResponseEntity<Void> internalDeposit(
            @Parameter(description = "ID пользователя", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID userId,
            @Valid @RequestBody DepositRequest request) {
        walletService.adminDeposit(userId.toString(), request);
        return ResponseEntity.ok().build();
    }
}