package com.orderservice.util;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @InjectMocks
    private OrderValidator orderValidator;

    private OrderDTO validOrderDTO;

    @BeforeEach
    void setUp() {
        OrderItemDTO validItem1 = new OrderItemDTO();
        validItem1.setProductId("PROD-001");
        validItem1.setQuantity(2);
        validItem1.setPrice(new BigDecimal("100.00"));

        OrderItemDTO validItem2 = new OrderItemDTO();
        validItem2.setProductId("PROD-002");
        validItem2.setQuantity(1);
        validItem2.setPrice(new BigDecimal("200.00"));

        validOrderDTO = new OrderDTO();
        validOrderDTO.setOrderNumber("TEST-ORDER-123");
        validOrderDTO.setItems(Arrays.asList(validItem1, validItem2));
    }

    @Test
    void validate_ShouldPassForValidOrder() {
        assertDoesNotThrow(() -> orderValidator.validate(validOrderDTO));
    }

    @Test
    void validate_ShouldThrowExceptionForNullItems() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(null);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("Um pedido deve ter pelo menos um item", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForEmptyItems() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Collections.emptyList());

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("Um pedido deve ter pelo menos um item", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForNullProductId() {
        OrderItemDTO invalidItem = new OrderItemDTO();
        invalidItem.setProductId(null);
        invalidItem.setQuantity(1);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Collections.singletonList(invalidItem));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("Todos os itens devem ter um ID de produto válido", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForEmptyProductId() {
        OrderItemDTO invalidItem = new OrderItemDTO();
        invalidItem.setProductId("");
        invalidItem.setQuantity(1);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Collections.singletonList(invalidItem));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("Todos os itens devem ter um ID de produto válido", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForZeroQuantity() {
        OrderItemDTO invalidItem = new OrderItemDTO();
        invalidItem.setProductId("PROD-001");
        invalidItem.setQuantity(0);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Collections.singletonList(invalidItem));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("A quantidade de cada item deve ser maior que zero", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForNegativeQuantity() {
        OrderItemDTO invalidItem = new OrderItemDTO();
        invalidItem.setProductId("PROD-001");
        invalidItem.setQuantity(-1);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Collections.singletonList(invalidItem));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("A quantidade de cada item deve ser maior que zero", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForNullQuantity() {
        OrderItemDTO invalidItem = new OrderItemDTO();
        invalidItem.setProductId("PROD-001");
        invalidItem.setQuantity(null);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Collections.singletonList(invalidItem));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("A quantidade de cada item deve ser maior que zero", exception.getMessage());
    }

    @Test
    void validate_ShouldThrowExceptionForDuplicateProducts() {
        OrderItemDTO item1 = new OrderItemDTO();
        item1.setProductId("PROD-001");
        item1.setQuantity(2);

        OrderItemDTO item2 = new OrderItemDTO();
        item2.setProductId("PROD-001");
        item2.setQuantity(1);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNumber("TEST-ORDER");
        orderDTO.setItems(Arrays.asList(item1, item2));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> orderValidator.validate(orderDTO));

        assertEquals("Produto duplicado no pedido: PROD-001", exception.getMessage());
    }
}