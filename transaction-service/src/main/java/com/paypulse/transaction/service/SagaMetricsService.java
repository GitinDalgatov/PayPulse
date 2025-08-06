package com.paypulse.transaction.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SagaMetricsService {

    private final Counter sagaTransactionsTotal;
    private final Counter sagaTransactionsSuccess;
    private final Counter sagaTransactionsFailed;
    private final Counter sagaCompensationsTotal;
    private final Timer sagaDurationTimer;

    public SagaMetricsService(MeterRegistry meterRegistry) {
        this.sagaTransactionsTotal = Counter.builder("saga_transactions_total")
                .description("Total number of Saga transactions")
                .register(meterRegistry);
        this.sagaTransactionsSuccess = Counter.builder("saga_transactions_success_total")
                .description("Total number of successful Saga transactions")
                .register(meterRegistry);
        this.sagaTransactionsFailed = Counter.builder("saga_transactions_failed_total")
                .description("Total number of failed Saga transactions")
                .register(meterRegistry);
        this.sagaCompensationsTotal = Counter.builder("saga_compensations_total")
                .description("Total number of Saga compensations")
                .register(meterRegistry);
        this.sagaDurationTimer = Timer.builder("saga_duration_seconds")
                .description("Saga transaction duration")
                .register(meterRegistry);
    }

    public void incrementTotalTransactions() {
        sagaTransactionsTotal.increment();
    }

    public void incrementSuccessfulTransactions() {
        sagaTransactionsSuccess.increment();
    }

    public void incrementFailedTransactions() {
        sagaTransactionsFailed.increment();
    }

    public void incrementCompensations() {
        sagaCompensationsTotal.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(sagaDurationTimer);
    }

    public void recordDuration(long duration, TimeUnit unit) {
        sagaDurationTimer.record(duration, unit);
    }
}