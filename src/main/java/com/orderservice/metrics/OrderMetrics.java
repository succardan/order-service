package com.orderservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Componente para registro de métricas relacionadas a pedidos
 * Facilita o monitoramento e alertas em ambientes de alta volumetria
 */
@Component
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    private Counter ordersReceivedCounter;
    private Counter ordersProcessedCounter;
    private Counter ordersNotifiedCounter;
    private Counter ordersErrorCounter;
    private Counter duplicateOrdersCounter;

    private Timer orderCreationTimer;
    private Timer orderProcessingTimer;
    private Timer orderNotificationTimer;
    private Timer externalServiceATimer;
    private Timer externalServiceBTimer;

    @PostConstruct
    public void init() {
        ordersReceivedCounter = Counter.builder("orders.received")
                .description("Número total de pedidos recebidos")
                .register(meterRegistry);

        ordersProcessedCounter = Counter.builder("orders.processed")
                .description("Número de pedidos processados com sucesso")
                .register(meterRegistry);

        ordersNotifiedCounter = Counter.builder("orders.notified")
                .description("Número de pedidos notificados ao sistema externo")
                .register(meterRegistry);

        ordersErrorCounter = Counter.builder("orders.error")
                .description("Número de pedidos com erro")
                .register(meterRegistry);

        duplicateOrdersCounter = Counter.builder("orders.duplicates")
                .description("Número de tentativas de envio de pedidos duplicados")
                .register(meterRegistry);

        orderCreationTimer = Timer.builder("orders.creation.time")
                .description("Tempo para criar um pedido")
                .register(meterRegistry);

        orderProcessingTimer = Timer.builder("orders.processing.time")
                .description("Tempo para processar um pedido")
                .register(meterRegistry);

        orderNotificationTimer = Timer.builder("orders.notification.time")
                .description("Tempo para notificar um pedido ao sistema externo")
                .register(meterRegistry);

        externalServiceATimer = Timer.builder("external.service.a.time")
                .description("Tempo de resposta do sistema externo A")
                .register(meterRegistry);

        externalServiceBTimer = Timer.builder("external.service.b.time")
                .description("Tempo de resposta do sistema externo B")
                .register(meterRegistry);
    }

    public void incrementOrdersReceived() {
        ordersReceivedCounter.increment();
    }

    public void incrementOrdersProcessed() {
        ordersProcessedCounter.increment();
    }

    public void incrementOrdersNotified() {
        ordersNotifiedCounter.increment();
    }

    public void incrementOrdersError() {
        ordersErrorCounter.increment();
    }

    public void incrementDuplicateOrders() {
        duplicateOrdersCounter.increment();
    }

    public void recordOrderCreationTime(long timeInMs) {
        orderCreationTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    public void recordOrderProcessingTime(long timeInMs) {
        orderProcessingTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    public void recordOrderNotificationTime(long timeInMs) {
        orderNotificationTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    public void recordExternalServiceATime(long timeInMs) {
        externalServiceATimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    public void recordExternalServiceBTime(long timeInMs) {
        externalServiceBTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }

    public <T> T measureOrderCreationTime(MeasuredOperation<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.execute();
        } finally {
            recordOrderCreationTime(System.currentTimeMillis() - startTime);
        }
    }

    public <T> T measureOrderProcessingTime(MeasuredOperation<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.execute();
        } finally {
            recordOrderProcessingTime(System.currentTimeMillis() - startTime);
        }
    }

    public <T> T measureOrderNotificationTime(MeasuredOperation<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.execute();
        } finally {
            recordOrderNotificationTime(System.currentTimeMillis() - startTime);
        }
    }

    public <T> T measureExternalServiceATime(MeasuredOperation<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.execute();
        } finally {
            recordExternalServiceATime(System.currentTimeMillis() - startTime);
        }
    }

    public <T> T measureExternalServiceBTime(MeasuredOperation<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.execute();
        } finally {
            recordExternalServiceBTime(System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Interface funcional para medição de operações
     */
    @FunctionalInterface
    public interface MeasuredOperation<T> {
        T execute();
    }
}