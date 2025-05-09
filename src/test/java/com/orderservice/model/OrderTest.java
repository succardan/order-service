package com.orderservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        order = new Order();

        item1 = new OrderItem();
        item1.setProductId("PROD-001");
        item1.setProductName("Product 1");
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("100.00"));

        item2 = new OrderItem();
        item2.setProductId("PROD-002");
        item2.setProductName("Product 2");
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("200.00"));
    }

    @Test
    void onCreate_ShouldSetDefaultValues() {
        order.onCreate();

        assertNotNull(order.getCreatedAt());
        assertEquals(OrderStatus.RECEIVED, order.getStatus());
        assertNotNull(order.getOrderNumber());
        assertEquals(10, order.getOrderNumber().length());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void onCreate_ShouldOnlySetDefaultsForNullValues() {
        OrderStatus status = OrderStatus.PROCESSING;
        String orderNumber = "EXISTING-123";
        BigDecimal totalAmount = new BigDecimal("500.00");

        order.setStatus(status);
        order.setOrderNumber(orderNumber);
        order.setTotalAmount(totalAmount);

        order.onCreate();
        assertNotNull(order.getCreatedAt());
        assertEquals(status, order.getStatus());
        assertEquals(orderNumber, order.getOrderNumber());
        assertEquals(totalAmount, order.getTotalAmount());
    }

    @Test
    void calculateTotal_ShouldSumAllItemTotals() {
        order.addItem(item1);
        order.addItem(item2);

        order.calculateTotal();

        assertEquals(new BigDecimal("400.00"), order.getTotalAmount());
    }

    @Test
    void calculateTotal_ShouldHandleEmptyItems() {
        order.calculateTotal();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void addItem_ShouldEstablishBidirectionalRelationship() {
        order.addItem(item1);

        assertTrue(order.getItems().contains(item1));
        assertEquals(order, item1.getOrder());
    }

    @Test
    void removeItem_ShouldRemoveRelationship() {
        order.addItem(item1);
        order.removeItem(item1);

        assertFalse(order.getItems().contains(item1));
        assertNull(item1.getOrder());
    }

    @Test
    void addItem_ShouldHandleMultipleItems() {
        order.addItem(item1);
        order.addItem(item2);

        assertEquals(2, order.getItems().size());
        assertTrue(order.getItems().contains(item1));
        assertTrue(order.getItems().contains(item2));
        assertEquals(order, item1.getOrder());
        assertEquals(order, item2.getOrder());
    }

    @Test
    void calculateTotal_ShouldHandleMultipleItems() {
        order.addItem(item1);
        order.addItem(item2);

        order.calculateTotal();

        BigDecimal expected = item1.getPrice().multiply(new BigDecimal(item1.getQuantity()))
                .add(item2.getPrice().multiply(new BigDecimal(item2.getQuantity())));

        assertEquals(expected, order.getTotalAmount());
    }

    @Test
    void calculateTotal_ShouldHandleZeroPriceItems() {
        item1.setPrice(BigDecimal.ZERO);
        order.addItem(item1);

        order.calculateTotal();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void onCreate_ShouldGenerateValidOrderNumber() {
        order.onCreate();

        String orderNumber = order.getOrderNumber();
        assertNotNull(orderNumber);
        assertEquals(10, orderNumber.length());
        assertTrue(orderNumber.matches("[A-Z0-9]{10}"));
    }

    @Test
    void onCreate_ShouldSetCreatedAtToCurrentTime() {
        LocalDateTime before = LocalDateTime.now();
        order.onCreate();
        LocalDateTime after = LocalDateTime.now();

        LocalDateTime createdAt = order.getCreatedAt();

        assertNotNull(createdAt);
        assertTrue(!createdAt.isBefore(before) && !createdAt.isAfter(after));
    }
}