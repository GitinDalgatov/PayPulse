package com.paypulse.transaction.service;

import com.paypulse.common.*;
import com.paypulse.transaction.entity.Transaction;
import com.paypulse.transaction.kafka.TransactionProducer;
import com.paypulse.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.micrometer.core.instrument.Timer;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSagaService {

    private final TransactionRepository transactionRepository;
    private final TransactionProducer producer;
    private final AuditService auditService;
    private final WebClient.Builder webClientBuilder;
    private final SagaMetricsService metricsService;
    private final OutboxService outboxService;

    private WebClient webClient() {
        return webClientBuilder.baseUrl("http://wallet-service:8082").build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public TransactionResponse executeTransaction(TransactionRequest request, String accessToken) {
        log.info("Starting Saga transaction: {} -> {} amount {}", request.fromUserId(), request.toUserId(), request.amount());
        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.error("Access token is null or empty");
            throw new RuntimeException("Access token is required");
        }
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        metricsService.incrementTotalTransactions();
        Timer.Sample timer = metricsService.startTimer();

        Transaction transaction = null;
        boolean fundsReserved = false;
        boolean fundsTransferred = false;

        try {
            log.info("Saga Step 1: Reserving funds for user {}", request.fromUserId());
            boolean reserveSuccess = reserveFunds(request.fromUserId(), request.amount(), accessToken);
            if (!reserveSuccess) throw new RuntimeException("Insufficient funds for user: " + request.fromUserId());
            fundsReserved = true;
            log.info("Saga Step 1 completed: Funds reserved for user {}", request.fromUserId());

            log.info("Saga Step 2: Transferring funds to user {}", request.toUserId());
            boolean transferSuccess = transferFunds(request.toUserId(), request.amount(), accessToken);
            if (!transferSuccess) throw new RuntimeException("Failed to transfer funds to user: " + request.toUserId());
            fundsTransferred = true;
            log.info("Saga Step 2 completed: Funds transferred to user {}", request.toUserId());

            log.info("Saga Step 3: Confirming transaction");
            transaction = confirmTransaction(request);
            log.info("Saga Step 3 completed: Transaction confirmed with ID {}", transaction.getId());

            log.info("Saga Step 4: Saving transaction event to outbox");
            TransactionCreatedEvent event = new TransactionCreatedEvent(
                    transaction.getFromUserId(),
                    transaction.getToUserId(),
                    transaction.getAmount(),
                    "TRANSACTION"
            );
            outboxService.saveTransactionEvent(transaction.getId().toString(), event);
            log.info("Saga Step 4 completed: Transaction event saved to outbox");

            auditService.logAction(request.fromUserId().toString(), "sagaTransaction",
                    "to=" + request.toUserId() + ", amount=" + request.amount() + ", status=SUCCESS");

            metricsService.incrementSuccessfulTransactions();
            metricsService.stopTimer(timer);

            log.info("Saga transaction completed successfully: {} -> {} amount {}",
                    request.fromUserId(), request.toUserId(), request.amount());

            return new TransactionResponse(
                    transaction.getId(), transaction.getFromUserId(), transaction.getToUserId(),
                    transaction.getAmount(), transaction.getTimestamp());

        } catch (Exception e) {
            log.error("Saga transaction failed: " + e.getMessage(), e);
            metricsService.incrementFailedTransactions();
            metricsService.stopTimer(timer);

            compensateTransaction(request, fundsReserved, fundsTransferred, accessToken);

            auditService.logAction(request.fromUserId().toString(), "sagaTransaction",
                    "to=" + request.toUserId() + ", amount=" + request.amount() + ", status=FAILED: " + e.getMessage());

            throw e;
        }
    }

    private boolean reserveFunds(UUID userId, BigDecimal amount, String accessToken) {
        try {
            WithdrawRequest withdrawRequest = new WithdrawRequest(amount, "Reserve for transaction");
            Boolean result = webClient().post()
                    .uri("/wallet/withdraw")
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(withdrawRequest)
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().value() == 200) return Mono.just(true);
                        if (resp.statusCode().value() == 400) {
                            log.warn("Insufficient funds for user {}", userId);
                            return Mono.just(false);
                        }
                        log.error("Unexpected status {} on reserve funds for user {}", resp.statusCode(), userId);
                        return resp.createException().flatMap(Mono::error);
                    }).block();
            return result != null && result;
        } catch (Exception e) {
            log.error("Error reserving funds for user {}", userId, e);
            return false;
        }
    }

    private boolean transferFunds(UUID userId, BigDecimal amount, String accessToken) {
        try {
            DepositRequest depositRequest = new DepositRequest(amount, "Transaction transfer");
            Boolean result = webClient().post()
                    .uri("/wallet/internal/deposit/" + userId)
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(depositRequest)
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().value() == 200) return Mono.just(true);
                        log.error("Failed to transfer funds to user {}, status: {}", userId, resp.statusCode());
                        return resp.createException().flatMap(Mono::error);
                    }).block();
            return result != null && result;
        } catch (Exception e) {
            log.error("Error transferring funds to user {}", userId, e);
            return false;
        }
    }

    @Transactional
    public Transaction confirmTransaction(TransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .fromUserId(request.fromUserId())
                .toUserId(request.toUserId())
                .amount(request.amount())
                .timestamp(Instant.now())
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
        return transactionRepository.save(transaction);
    }

    private void compensateTransaction(TransactionRequest request, boolean fundsReserved, boolean fundsTransferred, String accessToken) {
        log.info("Starting compensation for failed transaction: {} -> {} amount {}",
                request.fromUserId(), request.toUserId(), request.amount());
        try {
            if (fundsReserved && !fundsTransferred) {
                log.info("Compensation: Returning reserved funds to user {}", request.fromUserId());
                returnReservedFunds(request.fromUserId(), request.amount(), accessToken);
            }
            if (fundsTransferred) {
                log.info("Compensation: Rolling back transfer to user {}", request.toUserId());
                rollbackTransfer(request.toUserId(), request.amount(), accessToken);
                log.info("Compensation: Returning funds to user {}", request.fromUserId());
                returnReservedFunds(request.fromUserId(), request.amount(), accessToken);
            }
            auditService.logAction(request.fromUserId().toString(), "sagaCompensation",
                    "to=" + request.toUserId() + ", amount=" + request.amount() + ", status=COMPENSATED");
            metricsService.incrementCompensations();
            log.info("Compensation completed successfully for transaction: {} -> {} amount {}",
                    request.fromUserId(), request.toUserId(), request.amount());
        } catch (Exception e) {
            log.error("Compensation failed for transaction: {} -> {} amount {}",
                    request.fromUserId(), request.toUserId(), request.amount(), e);
            auditService.logAction(request.fromUserId().toString(), "sagaCompensationFailed",
                    "to=" + request.toUserId() + ", amount=" + request.amount() + ", error=" + e.getMessage());
            throw new RuntimeException("Compensation failed", e);
        }
    }

    private void returnReservedFunds(UUID userId, BigDecimal amount, String accessToken) {
        try {
            DepositRequest compensationRequest = new DepositRequest(amount, "Compensation for failed transaction");
            Boolean result = webClient().post()
                    .uri("/wallet/internal/deposit/" + userId)
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(compensationRequest)
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().value() == 200) return Mono.just(true);
                        log.error("Failed to return reserved funds to user {}, status: {}", userId, resp.statusCode());
                        return resp.createException().flatMap(Mono::error);
                    }).block();
            if (result != null && result) {
                log.info("Successfully returned reserved funds to user {}", userId);
            } else {
                log.error("Failed to return reserved funds to user {}", userId);
                throw new RuntimeException("Failed to return reserved funds");
            }
        } catch (Exception e) {
            log.error("Error returning reserved funds to user {}", userId, e);
            throw new RuntimeException("Failed to return reserved funds", e);
        }
    }

    private void rollbackTransfer(UUID userId, BigDecimal amount, String accessToken) {
        try {
            WithdrawRequest rollbackRequest = new WithdrawRequest(amount, "Rollback for failed transaction");
            Boolean result = webClient().post()
                    .uri("/wallet/withdraw")
                    .header("Authorization", "Bearer " + accessToken)
                    .bodyValue(rollbackRequest)
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().value() == 200) return Mono.just(true);
                        log.error("Failed to rollback transfer for user {}, status: {}", userId, resp.statusCode());
                        return resp.createException().flatMap(Mono::error);
                    }).block();
            if (result != null && result) {
                log.info("Successfully rolled back transfer for user {}", userId);
            } else {
                log.error("Failed to rollback transfer for user {}", userId);
                throw new RuntimeException("Failed to rollback transfer");
            }
        } catch (Exception e) {
            log.error("Error rolling back transfer for user {}", userId, e);
            throw new RuntimeException("Failed to rollback transfer", e);
        }
    }
}