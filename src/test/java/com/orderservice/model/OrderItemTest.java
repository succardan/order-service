package com.orderservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        orderItem = new OrderItem();
    }

    @Test
    void onCreate_ShouldSetDefaultValuesForNullPrice() {
        orderItem.setPrice(null);
        orderItem.onCreate();

        assertEquals(BigDecimal.ZERO, orderItem.getPrice());
    }

    @Test
    void onCreate_ShouldSetDefaultValuesForNullQuantity() {
        orderItem.setQuantity(null);
        orderItem.onCreate();

        assertEquals(0, orderItem.getQuantity());
    }

    @Test
    void onCreate_ShouldNotOverrideExistingValues() {
        BigDecimal price = new BigDecimal("123.45");
        orderItem.setPrice(price);
        orderItem.setQuantity(5);

        orderItem.onCreate();

        assertEquals(price, orderItem.getPrice());
        assertEquals(5, orderItem.getQuantity());
    }

    @Test
    void setOrder_ShouldEstablishRelationship() {
        Order order = new Order();

        orderItem.setOrder(order);

        assertEquals(order, orderItem.getOrder());
    }

    @Test
    void buildPattern_ShouldCreateCorrectInstance() {
        OrderItem item = OrderItem.builder()
                .productId("PROD-001")
                .productName("Test Product")
                .quantity(3)
                .price(new BigDecimal("100.00"))
                .build();

        assertEquals("PROD-001", item.getProductId());
        assertEquals("Test Product", item.getProductName());
        assertEquals(3, item.getQuantity());
        assertEquals(new BigDecimal("100.00"), item.getPrice());
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyInstance() {
        OrderItem item = new OrderItem();

        assertNull(item.getId());
        assertNull(item.getOrder());
        assertNull(item.getProductId());
        assertNull(item.getProductName());
        assertNull(item.getQuantity());
        assertNull(item.getPrice());
    }

    @Test
    void allArgsConstructor_ShouldCreatePopulatedInstance() {
        UUID id = UUID.randomUUID();
        Order order = new Order();
        String productId = "PROD-001";
        String productName = "Test Product";
        Integer quantity = 5;
        BigDecimal price = new BigDecimal("100.00");

        OrderItem item = new OrderItem(id, order, productId, productName, quantity, price);

        assertEquals(id, item.getId());
        assertEquals(order, item.getOrder());
        assertEquals(productId, item.getProductId());
        assertEquals(productName, item.getProductName());
        assertEquals(quantity, item.getQuantity());
        assertEquals(price, item.getPrice());
    }

    @Test
    void equals_ShouldReturnTrueForSameInstance() {
        assertEquals(orderItem, orderItem);
    }

    @Test
    void equals_ShouldReturnTrueForEqualInstances() {
        OrderItem item1 = new OrderItem();
        UUID id = UUID.randomUUID();
        item1.setId(id);
        item1.setProductId("PROD-001");
        item1.setQuantity(2);

        OrderItem item2 = new OrderItem();
        item2.setId(id);
        item2.setProductId("PROD-001");
        item2.setQuantity(2);

        assertEquals(item1, item2);
    }

    @Test
    void equals_ShouldReturnFalseForDifferentIds() {
        OrderItem item1 = new OrderItem();
        item1.setId(UUID.randomUUID());

        OrderItem item2 = new OrderItem();
        item2.setId(UUID.randomUUID());

        assertNotEquals(item1, item2);
    }

    @Test
    void equals_ShouldReturnFalseForDifferentClasses() {
        assertNotEquals(orderItem, new Object());
    }

    @Test
    void hashCode_ShouldBeConsistentWithEquals() {
        OrderItem item1 = new OrderItem();
        UUID id = UUID.randomUUID();
        item1.setId(id);

        OrderItem item2 = new OrderItem();
        item2.setId(id);

        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void toString_ShouldReturnNonNullString() {
        orderItem.setProductId("PROD-001");
        orderItem.setQuantity(2);

        String toString = orderItem.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("PROD-001"));
        assertTrue(toString.contains("2"));
    }
}