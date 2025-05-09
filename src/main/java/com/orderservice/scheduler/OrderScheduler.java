package com.orderservice.scheduler;

import com.orderservice.model.Order;
import com.orderservice.model.OrderStatus;
import com.orderservice.repository.OrderRepository;
import com.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agendador responsável por tarefas periódicas relacionadas a pedidos:
 * - Recuperação de pedidos com erro
 * - Reenvio de notificações pendentes
 * - Limpeza e manutenção
 * - Métricas e monitoramento
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Qualifier("orderProcessingExecutor")
    private final Executor orderProcessingExecutor;

    @Value("${app.scheduler.retry-limit:3}")
    private int retryLimit;

    @Value("${app.scheduler.batch-size:50}")
    private int batchSize;
    
    @Value("${app.scheduler.cleanup-enabled:false}")
    private boolean cleanupEnabled;

    /**
     * Processa pedidos pendentes (RECEIVED) que não foram processados
     * Executa a cada 1 minuto (ajustável para produção)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processReceivedOrders() {
        log.info("Iniciando processamento de pedidos pendentes (RECEIVED)");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.RECEIVED, cutoffTime, batchSize);

        if (pendingOrders.isEmpty()) {
            log.info("Nenhum pedido pendente encontrado");
            return;
        }

        log.info("Encontrados {} pedidos pendentes para processamento", pendingOrders.size());

        AtomicInteger processed = new AtomicInteger(0);

        pendingOrders.forEach(order -> CompletableFuture.runAsync(() -> {
            try {
                orderService.processOrder(order.getId());
                processed.incrementAndGet();
            } catch (Exception e) {
                log.error("Erro ao reprocessar pedido {}: {}", order.getId(), e.getMessage());
            }
        }, orderProcessingExecutor));

        log.info("Agendado reprocessamento de {} pedidos pendentes", pendingOrders.size());
    }

    /**
     * Notifica sistema externo de pedidos calculados mas não notificados
     * Executa a cada 5 minutos
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void notifyCalculatedOrders() {
        log.info("Iniciando notificação de pedidos calculados para o sistema externo B");

        List<Order> calculatedOrders = orderRepository.findByStatusAndNotifiedToExternalBFalse(
                OrderStatus.CALCULATED, batchSize);

        if (calculatedOrders.isEmpty()) {
            log.info("Nenhum pedido calculado pendente de notificação");
            return;
        }

        log.info("Encontrados {} pedidos calculados para notificação", calculatedOrders.size());

        AtomicInteger notified = new AtomicInteger(0);

        calculatedOrders.forEach(order -> CompletableFuture.runAsync(() -> {
            try {
                orderService.notifyExternalSystem(order.getId());
                notified.incrementAndGet();
            } catch (Exception e) {
                log.error("Erro ao notificar pedido {}: {}", order.getId(), e.getMessage());
            }
        }, orderProcessingExecutor));

        log.info("Agendada notificação de {} pedidos calculados", calculatedOrders.size());
    }

    /**
     * Tenta recuperar pedidos em estado de erro
     * Executa a cada 15 minutos
     */
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void recoverErroredOrders() {
        log.info("Iniciando recuperação de pedidos com erro");
        List<Order> erroredOrders = orderRepository.findByStatusAndRetryCountLessThan(
                OrderStatus.ERROR, retryLimit, batchSize);

        if (erroredOrders.isEmpty()) {
            log.info("Nenhum pedido com erro para recuperação");
            return;
        }

        log.info("Encontrados {} pedidos com erro para recuperação", erroredOrders.size());

        AtomicInteger recovered = new AtomicInteger(0);

        erroredOrders.forEach(order -> {
            order.setRetryCount(order.getRetryCount() + 1);
            order.setStatus(OrderStatus.RECEIVED);
            orderRepository.save(order);
            CompletableFuture.runAsync(() -> {
                        try {
                            orderService.processOrder(order.getId());
                            recovered.incrementAndGet();
                        } catch (Exception e) {
                            log.error("Erro ao recuperar pedido {}: {}", order.getId(), e.getMessage());
                        }
                    }, orderProcessingExecutor).orTimeout(30, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.error("Timeout ao recuperar pedido {}", order.getId());
                        return null;
                    });
        });

        log.info("Agendada recuperação de {} pedidos com erro", erroredOrders.size());
    }

    /**
     * Monitora métricas de pedidos e gera estatísticas
     * Executa a cada hora
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void generateOrderMetrics() {
        log.info("Gerando métricas de pedidos");

        LocalDateTime startOfHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfHour = startOfHour.plusHours(1);

        long totalOrders = orderRepository.countByCreatedAtBetween(startOfHour.minusHours(1), startOfHour);
        long processedOrders = orderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.NOTIFIED, startOfHour.minusHours(1), startOfHour);
        long errorOrders = orderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.ERROR, startOfHour.minusHours(1), startOfHour);

        double successRate = totalOrders > 0
                ? (double) processedOrders / totalOrders * 100
                : 0;

        log.info("Métricas da última hora - Total: {}, Processados: {}, Erros: {}, Taxa de Sucesso: {}%",
                totalOrders, processedOrders, errorOrders, successRate);
    }

    /**
     * Limpa pedidos muito antigos (opcional, para sistemas com retenção limitada)
     * Executa todos os dias às 03:00
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldOrders() {
        if (!cleanupEnabled) {
            return;
        }

        LocalDateTime retentionLimit = LocalDateTime.now().minusDays(180);

        int deleted = orderRepository.deleteCompletedOrdersOlderThan(retentionLimit);

        if (deleted > 0) {
            log.info("Limpeza de pedidos antigos: {} pedidos removidos", deleted);
        }
    }
}
