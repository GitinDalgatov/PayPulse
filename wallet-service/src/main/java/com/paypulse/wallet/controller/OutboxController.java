package com.paypulse.wallet.controller;

import com.paypulse.wallet.service.OutboxProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outbox")
@Tag(name = "Outbox", description = "Outbox monitoring endpoints")
public class OutboxController {

    private final OutboxProcessor outboxProcessor;

    public OutboxController(OutboxProcessor outboxProcessor) {
        this.outboxProcessor = outboxProcessor;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get outbox metrics", description = "Returns current outbox processing metrics")
    public OutboxProcessor.OutboxMetrics getMetrics() {
        return outboxProcessor.getMetrics();
    }
} 