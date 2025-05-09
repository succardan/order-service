package com.orderservice.util;

import com.orderservice.dto.OrderItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class OrderCalculatorTest {

    @InjectMocks
    private OrderCalculator orderCalculator;

    private OrderItemDTO item1;
    private OrderItemDTO item2;

    @BeforeEach
    void setUp() {
        item1 = new OrderItemDTO();
        item1.setProductId("PROD-001");
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("100.00"));

        item2 = new OrderItemDTO();
        item2.setProductId("PROD-002");
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("200.00"));
    }

    @Test
    void calculateTotal_ShouldSumAllItemTotals() {
        List<OrderItemDTO> items = Arrays.asList(item1, item2);

        BigDecimal total = orderCalculator.calculateTotal(items);

        assertEquals(new BigDecimal("400.00"), total);
    }

    @Test
    void calculateTotal_ShouldReturnZeroForNullItems() {
        BigDecimal total = orderCalculator.calculateTotal(null);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void calculateTotal_ShouldReturnZeroForEmptyItems() {
        BigDecimal total = orderCalculator.calculateTotal(Collections.emptyList());

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void calculateTotal_ShouldHandleDecimalPrices() {
        item1.setPrice(new BigDecimal("99.99"));
        item2.setPrice(new BigDecimal("149.95"));

        List<OrderItemDTO> items = Arrays.asList(item1, item2);

        BigDecimal total = orderCalculator.calculateTotal(items);
        BigDecimal expected = new BigDecimal("349.93");

        assertEquals(expected, total);
    }

    @Test
    void calculateItemTotal_ShouldMultiplyPriceByQuantity() {
        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(new BigDecimal("200.00"), total);
    }

    @Test
    void calculateItemTotal_ShouldReturnZeroForNullPrice() {
        item1.setPrice(null);

        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void calculateItemTotal_ShouldReturnZeroForNullQuantity() {
        item1.setQuantity(null);

        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void calculateItemTotal_ShouldHandleZeroQuantity() {
        item1.setQuantity(0);

        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void calculateItemTotal_ShouldHandleZeroPrice() {
        item1.setPrice(BigDecimal.ZERO);

        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void calculateItemTotal_ShouldHandleNegativeQuantity() {
        item1.setQuantity(-1);

        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(new BigDecimal("-100.00"), total);
    }

    @Test
    void calculateItemTotal_ShouldHandleNegativePrice() {
        item1.setPrice(new BigDecimal("-50.00"));

        BigDecimal total = orderCalculator.calculateItemTotal(item1);

        assertEquals(new BigDecimal("-100.00"), total);
    }

    @Test
    void calculateTotal_ShouldScaleToTwoDecimalPlaces() {
        item1.setPrice(new BigDecimal("33.333"));
        item1.setQuantity(3);

        List<OrderItemDTO> items = Collections.singletonList(item1);

        BigDecimal total = orderCalculator.calculateTotal(items);
        BigDecimal expected = new BigDecimal("100.00");

        assertEquals(expected, total);
        assertEquals(2, total.scale());
    }
}
