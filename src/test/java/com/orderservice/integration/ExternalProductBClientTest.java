package com.orderservice.integration;

import com.orderservice.integration.dto.ExternalOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExternalProductBClientTest {

    @Mock
    private ExternalProductBClient externalProductBClient;

    private ExternalOrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        orderDTO = new ExternalOrderDTO();
        orderDTO.setOrderNumber("ORD-TEST-123");
        orderDTO.setStatus("CALCULATED");
        orderDTO.setTotalAmount(new BigDecimal("300.00"));
        orderDTO.setItems(Collections.emptyList());
    }

    @Test
    void getOrderStatus_ShouldReturnOrderStatus() {
        when(externalProductBClient.getOrderStatus("ORD-TEST-123")).thenReturn("COMPLETED");

        String status = externalProductBClient.getOrderStatus("ORD-TEST-123");

        assertEquals("COMPLETED", status);
        verify(externalProductBClient).getOrderStatus("ORD-TEST-123");
    }

    @Test
    void notifyOrder_ShouldSendOrderNotification() {
        doNothing().when(externalProductBClient).notifyOrder(any(ExternalOrderDTO.class));

        assertDoesNotThrow(() -> externalProductBClient.notifyOrder(orderDTO));

        verify(externalProductBClient).notifyOrder(orderDTO);
    }

    @Test
    void getOrderStatus_ShouldHandleErrorResponses() {
        when(externalProductBClient.getOrderStatus("NONEXISTENT")).thenThrow(new RuntimeException("404 Not Found"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            externalProductBClient.getOrderStatus("NONEXISTENT");
        });

        assertEquals("404 Not Found", exception.getMessage());
        verify(externalProductBClient).getOrderStatus("NONEXISTENT");
    }

    @Test
    void notifyOrder_ShouldHandleErrorResponses() {
        doThrow(new RuntimeException("500 Internal Server Error"))
                .when(externalProductBClient).notifyOrder(any(ExternalOrderDTO.class));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            externalProductBClient.notifyOrder(orderDTO);
        });

        assertEquals("500 Internal Server Error", exception.getMessage());
        verify(externalProductBClient).notifyOrder(orderDTO);
    }
}