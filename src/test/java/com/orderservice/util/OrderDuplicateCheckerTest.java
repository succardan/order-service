package com.orderservice.util;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDuplicateCheckerTest {

    @Mock
    private OrderDuplicateChecker self;

    @InjectMocks
    private OrderDuplicateChecker duplicateChecker;

    private OrderDTO orderDTO1;
    private OrderDTO orderDTO2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(duplicateChecker, "self", self);

        OrderItemDTO item1 = new OrderItemDTO();
        item1.setProductId("PROD-001");
        item1.setQuantity(2);

        OrderItemDTO item2 = new OrderItemDTO();
        item2.setProductId("PROD-002");
        item2.setQuantity(1);

        orderDTO1 = new OrderDTO();
        orderDTO1.setOrderNumber("ORD-TEST-123");
        orderDTO1.setItems(Arrays.asList(item1, item2));

        OrderItemDTO item3 = new OrderItemDTO();
        item3.setProductId("PROD-001");
        item3.setQuantity(2);

        OrderItemDTO item4 = new OrderItemDTO();
        item4.setProductId("PROD-002");
        item4.setQuantity(1);

        orderDTO2 = new OrderDTO();
        orderDTO2.setOrderNumber("ORD-TEST-456");
        orderDTO2.setItems(Arrays.asList(item3, item4));
    }

    @Test
    void isOrderNumberDuplicate_ShouldReturnFalseForNewOrderNumber() {
        when(self.checkOrderNumberCache(anyString())).thenReturn(false);

        boolean result = duplicateChecker.isOrderNumberDuplicate("NEW-ORDER-123");

        assertFalse(result);
        verify(self).checkOrderNumberCache("NEW-ORDER-123");
        verify(self).markOrderNumberAsProcessed("NEW-ORDER-123");
    }

    @Test
    void isOrderNumberDuplicate_ShouldReturnTrueForExistingOrderNumber() {
        when(self.checkOrderNumberCache("EXISTING-ORDER")).thenReturn(true);

        boolean result = duplicateChecker.isOrderNumberDuplicate("EXISTING-ORDER");

        assertTrue(result);
        verify(self).checkOrderNumberCache("EXISTING-ORDER");
        verify(self, never()).markOrderNumberAsProcessed(anyString());
    }

    @Test
    void isOrderContentDuplicate_ShouldReturnFalseForNewContent() {
        when(self.checkOrderHashCache(anyString())).thenReturn(false);

        boolean result = duplicateChecker.isOrderContentDuplicate(orderDTO1);

        assertFalse(result);
        verify(self).checkOrderHashCache(anyString());
        verify(self).markOrderHashAsProcessed(anyString());
    }

    @Test
    void isOrderContentDuplicate_ShouldReturnTrueForDuplicateContent() {
        when(self.checkOrderHashCache(anyString())).thenReturn(true);

        boolean result = duplicateChecker.isOrderContentDuplicate(orderDTO2);

        assertTrue(result);
        verify(self).checkOrderHashCache(anyString());
        verify(self, never()).markOrderHashAsProcessed(anyString());
    }

    @Test
    void checkOrderNumberCache_ShouldReturnDefaultValue() {
        Boolean result = duplicateChecker.checkOrderNumberCache("TEST-ORDER");

        assertFalse(result);
    }

    @Test
    void markOrderNumberAsProcessed_ShouldReturnTrue() {
        Boolean result = duplicateChecker.markOrderNumberAsProcessed("TEST-ORDER");

        assertTrue(result);
    }

    @Test
    void checkOrderHashCache_ShouldReturnDefaultValue() {
        Boolean result = duplicateChecker.checkOrderHashCache("HASH123");

        assertFalse(result);
    }

    @Test
    void markOrderHashAsProcessed_ShouldReturnTrue() {
        Boolean result = duplicateChecker.markOrderHashAsProcessed("HASH123");

        assertTrue(result);
    }

    @Test
    void isOrderContentDuplicate_ShouldHandleNullOrEmptyItems() {
        OrderDTO emptyDTO = new OrderDTO();

        assertFalse(duplicateChecker.isOrderContentDuplicate(null));
        assertFalse(duplicateChecker.isOrderContentDuplicate(emptyDTO));

        emptyDTO.setItems(null);
        assertFalse(duplicateChecker.isOrderContentDuplicate(emptyDTO));
    }
}