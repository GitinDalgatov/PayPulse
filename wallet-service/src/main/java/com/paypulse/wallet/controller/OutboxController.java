package com.paypulse.wallet.controller;

import com.paypulse.wallet.service.OutboxProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Outbox", description = "Outbox monitoring endpoints")
@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxProcessor outboxProcessor;

    @GetMapping("/metrics")
    @Operation(summary = "Get outbox metrics", description = "Returns current outbox processing metrics")
    public OutboxProcessor.OutboxMetrics getMetrics() {
        log.info("Received request for outbox metrics");
        return outboxProcessor.getMetrics();
    }
}