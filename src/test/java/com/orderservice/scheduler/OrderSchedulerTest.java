package com.orderservice.scheduler;

import com.orderservice.model.Order;
import com.orderservice.model.OrderStatus;
import com.orderservice.repository.OrderRepository;
import com.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class OrderSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private Executor orderProcessingExecutor;

    @InjectMocks
    private OrderScheduler orderScheduler;

    private Order receivedOrder;
    private Order calculatedOrder;
    private Order errorOrder;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(orderProcessingExecutor).execute(any(Runnable.class));

        ReflectionTestUtils.setField(orderScheduler, "retryLimit", 3);
        ReflectionTestUtils.setField(orderScheduler, "batchSize", 50);
        ReflectionTestUtils.setField(orderScheduler, "cleanupEnabled", false);

        receivedOrder = new Order();
        receivedOrder.setId(UUID.randomUUID());
        receivedOrder.setOrderNumber("RCV-ORDER");
        receivedOrder.setStatus(OrderStatus.RECEIVED);
        receivedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        calculatedOrder = new Order();
        calculatedOrder.setId(UUID.randomUUID());
        calculatedOrder.setOrderNumber("CALC-ORDER");
        calculatedOrder.setStatus(OrderStatus.CALCULATED);
        calculatedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        calculatedOrder.setNotifiedToExternalB(false);

        errorOrder = new Order();
        errorOrder.setId(UUID.randomUUID());
        errorOrder.setOrderNumber("ERR-ORDER");
        errorOrder.setStatus(OrderStatus.ERROR);
        errorOrder.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        errorOrder.setRetryCount(1);
    }

    @Test
    void processReceivedOrders_ShouldProcessPendingOrders() {
        when(orderRepository.findByStatusAndCreatedAtBefore(
                eq(OrderStatus.RECEIVED), any(LocalDateTime.class), eq(50)))
                .thenReturn(Collections.singletonList(receivedOrder));

        orderScheduler.processReceivedOrders();

        verify(orderRepository).findByStatusAndCreatedAtBefore(
                eq(OrderStatus.RECEIVED), any(LocalDateTime.class), eq(50));
        verify(orderService).processOrder(receivedOrder.getId());
        verify(orderProcessingExecutor).execute(any(Runnable.class));
    }

    @Test
    void processReceivedOrders_ShouldHandleNoOrders() {
        when(orderRepository.findByStatusAndCreatedAtBefore(
                eq(OrderStatus.RECEIVED), any(LocalDateTime.class), eq(50)))
                .thenReturn(Collections.emptyList());

        orderScheduler.processReceivedOrders();

        verify(orderRepository).findByStatusAndCreatedAtBefore(
                eq(OrderStatus.RECEIVED), any(LocalDateTime.class), eq(50));
        verify(orderService, never()).processOrder(any(UUID.class));
        verify(orderProcessingExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void notifyCalculatedOrders_ShouldNotifyPendingOrders() {
        when(orderRepository.findByStatusAndNotifiedToExternalBFalse(
                eq(OrderStatus.CALCULATED), eq(50)))
                .thenReturn(Collections.singletonList(calculatedOrder));

        orderScheduler.notifyCalculatedOrders();

        verify(orderRepository).findByStatusAndNotifiedToExternalBFalse(
                eq(OrderStatus.CALCULATED), eq(50));
        verify(orderService).notifyExternalSystem(calculatedOrder.getId());
        verify(orderProcessingExecutor).execute(any(Runnable.class));
    }

    @Test
    void notifyCalculatedOrders_ShouldHandleNoOrders() {
        when(orderRepository.findByStatusAndNotifiedToExternalBFalse(
                eq(OrderStatus.CALCULATED), eq(50)))
                .thenReturn(Collections.emptyList());

        orderScheduler.notifyCalculatedOrders();

        verify(orderRepository).findByStatusAndNotifiedToExternalBFalse(
                eq(OrderStatus.CALCULATED), eq(50));
        verify(orderService, never()).notifyExternalSystem(any(UUID.class));
        verify(orderProcessingExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void recoverErroredOrders_ShouldRecoverOrdersWithinRetryLimit() {
        when(orderRepository.findByStatusAndRetryCountLessThan(
                eq(OrderStatus.ERROR), eq(3), eq(50)))
                .thenReturn(Collections.singletonList(errorOrder));

        orderScheduler.recoverErroredOrders();

        verify(orderRepository).findByStatusAndRetryCountLessThan(
                eq(OrderStatus.ERROR), eq(3), eq(50));
        verify(orderRepository).save(errorOrder);
        verify(orderService).processOrder(errorOrder.getId());
        verify(orderProcessingExecutor).execute(any(Runnable.class));

        assertEquals(OrderStatus.RECEIVED, errorOrder.getStatus());
        assertEquals(2, errorOrder.getRetryCount());
    }

    @Test
    void recoverErroredOrders_ShouldHandleNoOrders() {
        when(orderRepository.findByStatusAndRetryCountLessThan(
                eq(OrderStatus.ERROR), eq(3), eq(50)))
                .thenReturn(Collections.emptyList());

        orderScheduler.recoverErroredOrders();

        verify(orderRepository).findByStatusAndRetryCountLessThan(
                eq(OrderStatus.ERROR), eq(3), eq(50));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderService, never()).processOrder(any(UUID.class));
        verify(orderProcessingExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void generateOrderMetrics_ShouldGenerateMetrics() {

        when(orderRepository.countByCreatedAtBetween(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(100L);

        when(orderRepository.countByStatusAndCreatedAtBetween(
                eq(OrderStatus.NOTIFIED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(90L);

        when(orderRepository.countByStatusAndCreatedAtBetween(
                eq(OrderStatus.ERROR), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);

        orderScheduler.generateOrderMetrics();

        verify(orderRepository).countByCreatedAtBetween(
                any(LocalDateTime.class), any(LocalDateTime.class));

        verify(orderRepository).countByStatusAndCreatedAtBetween(
                eq(OrderStatus.NOTIFIED), any(LocalDateTime.class), any(LocalDateTime.class));

        verify(orderRepository).countByStatusAndCreatedAtBetween(
                eq(OrderStatus.ERROR), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void cleanupOldOrders_ShouldNotDeleteWhenCleanupDisabled() {
        ReflectionTestUtils.setField(orderScheduler, "cleanupEnabled", false);

        orderScheduler.cleanupOldOrders();

        verify(orderRepository, never()).deleteCompletedOrdersOlderThan(any(LocalDateTime.class));
    }

    @Test
    void processReceivedOrders_ShouldHandleProcessingErrors() {
        when(orderRepository.findByStatusAndCreatedAtBefore(
                eq(OrderStatus.RECEIVED), any(LocalDateTime.class), eq(50)))
                .thenReturn(Collections.singletonList(receivedOrder));

        doThrow(new RuntimeException("Test processing error"))
                .when(orderService).processOrder(receivedOrder.getId());

        orderScheduler.processReceivedOrders();

        verify(orderRepository).findByStatusAndCreatedAtBefore(
                eq(OrderStatus.RECEIVED), any(LocalDateTime.class), eq(50));
        verify(orderService).processOrder(receivedOrder.getId());
    }

    @Test
    void notifyCalculatedOrders_ShouldHandleNotificationErrors() {
        when(orderRepository.findByStatusAndNotifiedToExternalBFalse(
                eq(OrderStatus.CALCULATED), eq(50)))
                .thenReturn(Collections.singletonList(calculatedOrder));

        doThrow(new RuntimeException("Test notification error"))
                .when(orderService).notifyExternalSystem(calculatedOrder.getId());

        orderScheduler.notifyCalculatedOrders();

        verify(orderRepository).findByStatusAndNotifiedToExternalBFalse(
                eq(OrderStatus.CALCULATED), eq(50));
        verify(orderService).notifyExternalSystem(calculatedOrder.getId());
    }
}
