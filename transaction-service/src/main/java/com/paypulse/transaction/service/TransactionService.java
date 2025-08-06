package com.paypulse.transaction.service;

import com.paypulse.common.TransactionRequest;
import com.paypulse.common.TransactionResponse;
import com.paypulse.transaction.entity.Transaction;
import com.paypulse.transaction.kafka.TransactionProducer;
import com.paypulse.transaction.repository.TransactionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.paypulse.common.AuditService;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;
    private final TransactionProducer producer;
    private final AuditService auditService;
    private final TransactionSagaService sagaService;

    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public TransactionResponse create(TransactionRequest request) {
        log.info("Starting Saga transaction: {} -> {} amount {}", request.fromUserId(), request.toUserId(), request.amount());
        String accessToken = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() != null) {
                accessToken = auth.getCredentials().toString();
            }
            if (accessToken == null || accessToken.trim().isEmpty()) {
                log.warn("Access token not found in authentication context");
            }
        } catch (Exception e) {
            log.error("Error extracting access token", e);
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No valid access token provided");
        }
        try {
            return sagaService.executeTransaction(request, accessToken);
        } catch (Exception e) {
            log.error("Saga transaction failed: " + e.getMessage(), e);
            auditService.logAction(request.fromUserId().toString(), "createTransaction",
                    "to=" + request.toUserId() + ", amount=" + request.amount() + ", status=FAILED: " + e.getMessage());
            throw e;
        }
    }

    public List<TransactionResponse> getHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        UUID userId = UUID.fromString(auth.getName());
        List<Transaction> list = repository.findAllByFromUserIdOrToUserIdOrderByTimestampDesc(userId, userId);
        return list.stream()
                .map(tx -> new TransactionResponse(
                        tx.getId(), tx.getFromUserId(), tx.getToUserId(),
                        tx.getAmount(), tx.getTimestamp()))
                .collect(Collectors.toList());
    }
}