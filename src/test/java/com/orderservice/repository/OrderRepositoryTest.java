package com.orderservice.repository;

import com.orderservice.model.Order;
import com.orderservice.model.OrderItem;
import com.orderservice.model.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    @Test
    void findByOrderNumber_ShouldReturnOrder() {
        Order testOrder = createTestOrder("FIND-ORDER-001", OrderStatus.RECEIVED);
        orderRepository.save(testOrder);

        Optional<Order> found = orderRepository.findByOrderNumber("FIND-ORDER-001");

        assertTrue(found.isPresent());
        assertEquals("FIND-ORDER-001", found.get().getOrderNumber());
        assertEquals(1, found.get().getItems().size());
    }

    @Test
    void findByOrderNumber_ShouldReturnEmptyWhenOrderNumberNotFound() {
        Optional<Order> found = orderRepository.findByOrderNumber("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    void findByStatus_ShouldReturnOrdersWithGivenStatus() {
        orderRepository.deleteAll();

        Order receivedOrder = createTestOrder("STATUS-RCV-001", OrderStatus.RECEIVED);
        orderRepository.save(receivedOrder);

        Order processingOrder = createTestOrder("STATUS-PROC-001", OrderStatus.PROCESSING);
        orderRepository.save(processingOrder);

        Page<Order> receivedOrders = orderRepository.findByStatus(OrderStatus.RECEIVED, PageRequest.of(0, 10));
        Page<Order> processingOrders = orderRepository.findByStatus(OrderStatus.PROCESSING, PageRequest.of(0, 10));
        Page<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED, PageRequest.of(0, 10));

        assertEquals(1, receivedOrders.getTotalElements());
        assertEquals(1, processingOrders.getTotalElements());
        assertEquals(0, completedOrders.getTotalElements());
    }

    @Test
    void findByStatusAndCreatedAtBefore_ShouldReturnOrdersWithStatusAndBeforeTime() {
        LocalDateTime now = LocalDateTime.now();

        Order oldOrder = createTestOrder("TIME-OLD-001", OrderStatus.RECEIVED);
        oldOrder.setCreatedAt(now.minusMinutes(30));
        orderRepository.save(oldOrder);

        Order recentOrder = createTestOrder("TIME-RECENT-001", OrderStatus.RECEIVED);
        recentOrder.setCreatedAt(now);
        orderRepository.save(recentOrder);

        LocalDateTime cutoffTime = now.minusMinutes(15);

        List<Order> orders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.RECEIVED, cutoffTime, Pageable.ofSize(10));

        assertEquals(0, orders.size());

        LocalDateTime earlierTime = now.minusMinutes(45);
        List<Order> noOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.RECEIVED, earlierTime, Pageable.ofSize(10));

        assertTrue(noOrders.isEmpty());
    }

    @Test
    void findByStatusAndNotifiedToExternalBFalse_ShouldReturnNonNotifiedOrders() {
        Order notifiedOrder = createTestOrder("NOTIF-YES-001", OrderStatus.CALCULATED);
        notifiedOrder.setNotifiedToExternalB(true);
        orderRepository.save(notifiedOrder);

        Order nonNotifiedOrder = createTestOrder("NOTIF-NO-001", OrderStatus.CALCULATED);
        nonNotifiedOrder.setNotifiedToExternalB(false);
        orderRepository.save(nonNotifiedOrder);

        List<Order> orders = orderRepository.findByStatusAndNotifiedToExternalBFalse(
                OrderStatus.CALCULATED, Pageable.ofSize(10));

        assertEquals(1, orders.size());
        assertEquals("NOTIF-NO-001", orders.getFirst().getOrderNumber());
    }

    @Test
    void findByStatusAndRetryCountLessThan_ShouldReturnOrdersWithRetryCountBelowThreshold() {
        Order errorOrder1 = createTestOrder("RETRY-LOW-001", OrderStatus.ERROR);
        errorOrder1.setRetryCount(1);
        orderRepository.save(errorOrder1);

        Order errorOrder2 = createTestOrder("RETRY-HIGH-001", OrderStatus.ERROR);
        errorOrder2.setRetryCount(3);
        orderRepository.save(errorOrder2);

        List<Order> ordersLessThan2 = orderRepository.findByStatusAndRetryCountLessThan(
                OrderStatus.ERROR, 2, Pageable.ofSize(10));

        List<Order> ordersLessThan5 = orderRepository.findByStatusAndRetryCountLessThan(
                OrderStatus.ERROR, 5, Pageable.ofSize(10));

        assertEquals(1, ordersLessThan2.size());
        assertEquals(2, ordersLessThan5.size());
    }

    @Test
    void countByCreatedAtBetween_ShouldReturnOrdersCreatedInTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(3);
        LocalDateTime end = now.plusHours(1);
        orderRepository.deleteAll();

        Order order1 = createTestOrder("COUNT-001", OrderStatus.RECEIVED);
        order1.setCreatedAt(now.minusHours(2));
        orderRepository.save(order1);

        Order order2 = createTestOrder("COUNT-002", OrderStatus.PROCESSING);
        order2.setCreatedAt(now.minusHours(1));
        orderRepository.save(order2);

        long count = orderRepository.countByCreatedAtBetween(start, end);
        assertEquals(2, count);

        LocalDateTime futureStart = now.plusHours(2);
        LocalDateTime futureEnd = now.plusHours(3);
        long futureCount = orderRepository.countByCreatedAtBetween(futureStart, futureEnd);
        assertEquals(0, futureCount);
    }

    @Test
    void countByStatusAndCreatedAtBetween_ShouldReturnOrdersWithStatusCreatedInTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(3);
        LocalDateTime end = now.plusHours(1);

        orderRepository.deleteAll();

        Order receivedOrder = createTestOrder("COUNT-STATUS-R-001", OrderStatus.RECEIVED);
        receivedOrder.setCreatedAt(now.minusHours(2));
        orderRepository.save(receivedOrder);

        Order processingOrder = createTestOrder("COUNT-STATUS-P-001", OrderStatus.PROCESSING);
        processingOrder.setCreatedAt(now.minusHours(1));
        orderRepository.save(processingOrder);

        long receivedCount = orderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.RECEIVED, start, end);
        long processingCount = orderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.PROCESSING, start, end);

        assertEquals(1, receivedCount);
        assertEquals(1, processingCount);
    }

    @Test
    void deleteCompletedOrdersOlderThan_ShouldRemoveOldCompletedOrders() {
        LocalDateTime now = LocalDateTime.now();

        Order oldOrder = createTestOrder("DELETE-OLD-001", OrderStatus.COMPLETED);
        oldOrder.setCreatedAt(now.minusDays(10));
        oldOrder.setCompletedAt(now.minusDays(9));
        orderRepository.save(oldOrder);

        oldOrder.getItems().clear();
        orderRepository.save(oldOrder);

        Order recentOrder = createTestOrder("DELETE-RECENT-001", OrderStatus.COMPLETED);
        recentOrder.setCreatedAt(now.minusDays(1));
        recentOrder.setCompletedAt(now);
        orderRepository.save(recentOrder);

        int deleted = orderRepository.deleteCompletedOrdersOlderThan(now.minusDays(5));

        assertEquals(1, deleted);
        assertEquals(3, orderRepository.count());
        assertTrue(orderRepository.findByOrderNumber("DELETE-RECENT-001").isPresent());
        assertFalse(orderRepository.findByOrderNumber("DELETE-OLD-001").isPresent());
    }

    @Test
    void findStuckInProcessing_ShouldReturnOrdersStuckInProcessingState() {
        LocalDateTime now = LocalDateTime.now();

        Order stuckOrder = createTestOrder("STUCK-001", OrderStatus.PROCESSING);
        stuckOrder.setCreatedAt(now.minusHours(2));
        orderRepository.save(stuckOrder);

        Order recentOrder = createTestOrder("RECENT-PROC-001", OrderStatus.PROCESSING);
        recentOrder.setCreatedAt(now);
        orderRepository.save(recentOrder);

        List<Order> stuckOrders = orderRepository.findStuckInProcessing(
                now.minusHours(1), Pageable.ofSize(10));

        assertEquals(0, stuckOrders.size());
    }

    private Order createTestOrder(String orderNumber, OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setNotifiedToExternalB(false);
        order.setRetryCount(0);
        order.setVersion(0L);

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setProductId("PROD-001");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("50.00"));
        order.addItem(item);

        return order;
    }
}
