package com.paypulse.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    public void logAction(String userId, String action, String details) {
        log.info("AUDIT | user={} | action={} | details={} | at={}", userId, action, details, Instant.now());
    }
} 