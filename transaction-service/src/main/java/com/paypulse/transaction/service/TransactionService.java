package com.paypulse.transaction.service;

import com.paypulse.common.TransactionRequest;
import com.paypulse.common.TransactionResponse;
import com.paypulse.transaction.entity.Transaction;
import com.paypulse.transaction.kafka.TransactionProducer;
import com.paypulse.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import com.paypulse.common.WithdrawRequest;
import com.paypulse.common.DepositRequest;
import reactor.core.publisher.Mono;
import com.paypulse.common.AuditService;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository repository;
    private final TransactionProducer producer;
    private final WebClient webClient = WebClient.builder()
        .baseUrl("http://wallet-service:8082")
        .build();
    private final AuditService auditService;
    private final TransactionSagaService sagaService;

    public TransactionService(TransactionRepository repository, TransactionProducer producer, 
                           AuditService auditService, TransactionSagaService sagaService) {
        this.repository = repository;
        this.producer = producer;
        this.auditService = auditService;
        this.sagaService = sagaService;
    }

    @Transactional(rollbackFor = Exception.class)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public com.paypulse.common.TransactionResponse create(com.paypulse.common.TransactionRequest request) {
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



    public List<com.paypulse.common.TransactionResponse> getHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        UUID userId = UUID.fromString(auth.getName());
        List<Transaction> list = repository.findAllByFromUserIdOrToUserIdOrderByTimestampDesc(userId, userId);
        return list.stream()
                .map(tx -> new com.paypulse.common.TransactionResponse(
                    tx.getId(), tx.getFromUserId(), tx.getToUserId(), 
                    tx.getAmount(), tx.getTimestamp()))
                .collect(Collectors.toList());
    }
}