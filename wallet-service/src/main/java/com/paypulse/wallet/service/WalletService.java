package com.paypulse.wallet.service;

import com.paypulse.common.*;
import com.paypulse.wallet.entity.TransactionHistory;
import com.paypulse.wallet.entity.Wallet;
import com.paypulse.wallet.repository.HistoryRepository;
import com.paypulse.wallet.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final HistoryRepository historyRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            log.error("User not authenticated");
            throw new IllegalStateException("User not authenticated");
        }
        return UUID.fromString(auth.getName());
    }

    private void ensureWalletExists(UUID userId) {
        if (!walletRepository.existsById(userId)) {
            walletRepository.save(Wallet.builder()
                    .userId(userId)
                    .balance(BigDecimal.ZERO)
                    .build());
            entityManager.flush();
        }
    }

    private void saveTransactionHistory(UUID userId, BigDecimal amount, String description) {
        historyRepository.save(TransactionHistory.builder()
                .userId(userId)
                .amount(amount)
                .description(description)
                .timestamp(Instant.now())
                .build());
    }

    private void publishBalanceEvent(UUID userId, String type, String description) {
        outboxService.saveBalanceEvent(userId.toString(),
                new BalanceChangedEvent(userId, type, description));
    }

    private void updateWalletBalance(UUID userId,
                                     BigDecimal amount,
                                     String description,
                                     String operationType) {

        Wallet wallet = entityManager.find(Wallet.class, userId, LockModeType.PESSIMISTIC_WRITE);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        saveTransactionHistory(userId, amount, description);
        auditService.logAction(userId.toString(), operationType, amount + " " + description);
        log.info("User {} {} {}. New balance: {}", userId, operationType.toLowerCase(), amount, wallet.getBalance());

        publishBalanceEvent(userId, operationType, description);
    }

    @Transactional
    public BalanceResponse getBalance() {
        UUID userId = getCurrentUserId();
        Wallet wallet = walletRepository.findById(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build()));
        auditService.logAction(userId.toString(), "getBalance", "balance=" + wallet.getBalance());
        log.info("User {} balance: {}", userId, wallet.getBalance());
        return new BalanceResponse(wallet.getBalance());
    }

    @Transactional
    public void deposit(DepositRequest request) {
        UUID userId = getCurrentUserId();
        ensureWalletExists(userId);
        updateWalletBalance(userId, request.amount(), request.description(), "DEPOSIT");
    }

    @Transactional
    public boolean withdraw(WithdrawRequest request) {
        UUID userId = getCurrentUserId();
        ensureWalletExists(userId);

        Wallet wallet = entityManager.find(Wallet.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            log.warn("User {} insufficient funds: {} < {}", userId, wallet.getBalance(), request.amount());
            return false;
        }

        updateWalletBalance(userId, request.amount().negate(), request.description(), "WITHDRAW");
        return true;
    }

    public Page<HistoryResponse> getHistory(Pageable pageable) {
        UUID userId = getCurrentUserId();
        return historyRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(tx -> new HistoryResponse(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getDescription(),
                        tx.getTimestamp()
                ));
    }

    public Page<HistoryResponse> getAllHistory(Pageable pageable) {
        return historyRepository.findAll(pageable)
                .map(tx -> new HistoryResponse(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getDescription(),
                        tx.getTimestamp()
                ));
    }

    @Transactional
    public void adminDeposit(String userIdStr, DepositRequest request) {
        UUID userId = UUID.fromString(userIdStr);
        ensureWalletExists(userId);
        updateWalletBalance(userId, request.amount(), request.description(), "ADMIN_DEPOSIT");
    }
}