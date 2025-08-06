package com.paypulse.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class AuditService {
    public void logAction(String userId, String action, String details) {
        log.info("AUDIT | user={} | action={} | details={} | at={}", userId, action, details, Instant.now());
    }
}