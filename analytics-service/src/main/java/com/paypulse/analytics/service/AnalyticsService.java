package com.paypulse.analytics.service;

import com.paypulse.analytics.repository.BalanceRepository;
import com.paypulse.analytics.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;

    public Map<String, Object> getUserAnalytics() {
        try {
            log.info("Analytics: getUserAnalytics called");

            Map<String, Object> data = new HashMap<>();
            data.put("transactionCount", 0);
            data.put("totalAmount", 0.0);

            log.info("Analytics: data={}", data);
            return data;
        } catch (Exception e) {
            log.error("Error in getUserAnalytics", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("transactionCount", 0);
            errorData.put("totalAmount", 0.0);
            return errorData;
        }
    }

    public List<Map<String, Object>> getBalanceEvents() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                throw new IllegalStateException("User not authenticated");
            }

            UUID userId = UUID.fromString(auth.getName());
            log.info("Analytics: getBalanceEvents for userId={}", userId);

            return balanceRepository.findByUserId(userId).stream()
                    .map(event -> {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("id", event.id());
                        eventMap.put("userId", event.userId());
                        eventMap.put("type", event.type());
                        eventMap.put("description", event.description());
                        return eventMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in getBalanceEvents", e);
            throw e;
        }
    }

    public List<Map<String, Object>> getTransactionEvents() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                throw new IllegalStateException("User not authenticated");
            }

            UUID userId = UUID.fromString(auth.getName());
            log.info("Analytics: getTransactionEvents for userId={}", userId);

            return transactionRepository.findByFromUserIdOrToUserId(userId, userId).stream()
                    .map(event -> {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("id", event.id());
                        eventMap.put("fromUserId", event.fromUserId());
                        eventMap.put("toUserId", event.toUserId());
                        eventMap.put("amount", event.amount());
                        eventMap.put("type", event.type());
                        return eventMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in getTransactionEvents", e);
            throw e;
        }
    }
}