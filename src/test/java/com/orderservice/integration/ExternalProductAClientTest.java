package com.orderservice.integration;

import com.orderservice.integration.dto.ExternalOrderDTO;
import com.orderservice.integration.dto.ExternalProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExternalProductAClientTest {

    @Mock
    private ExternalProductAClient externalProductAClient;

    private ExternalProductDTO product1;
    private ExternalProductDTO product2;
    private List<ExternalProductDTO> productList;

    @BeforeEach
    void setUp() {
        product1 = new ExternalProductDTO();
        product1.setId("PROD-001");
        product1.setName("Product 1");
        product1.setDescription("Description 1");
        product1.setPrice(new BigDecimal("100.00"));
        product1.setAvailable(true);

        product2 = new ExternalProductDTO();
        product2.setId("PROD-002");
        product2.setName("Product 2");
        product2.setDescription("Description 2");
        product2.setPrice(new BigDecimal("200.00"));
        product2.setAvailable(false);

        productList = Arrays.asList(product1, product2);
    }

    @Test
    void getProduct_ShouldReturnProductWhenExists() {
        when(externalProductAClient.getProduct("PROD-001")).thenReturn(product1);

        ExternalProductDTO result = externalProductAClient.getProduct("PROD-001");

        assertNotNull(result);
        assertEquals("PROD-001", result.getId());
        assertEquals("Product 1", result.getName());
        assertEquals(new BigDecimal("100.00"), result.getPrice());
        assertTrue(result.isAvailable());

        verify(externalProductAClient).getProduct("PROD-001");
    }

    @Test
    void getProducts_ShouldReturnAllProducts() {
        when(externalProductAClient.getProducts()).thenReturn(productList);

        List<ExternalProductDTO> results = externalProductAClient.getProducts();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("PROD-001", results.get(0).getId());
        assertEquals("PROD-002", results.get(1).getId());

        verify(externalProductAClient).getProducts();
    }

    @Test
    void submitOrder_ShouldSubmitOrderToExternalSystem() {
        ExternalOrderDTO orderRequest = new ExternalOrderDTO();
        orderRequest.setOrderNumber("ORD-TEST-123");
        orderRequest.setStatus("RECEIVED");
        orderRequest.setTotalAmount(new BigDecimal("300.00"));
        orderRequest.setItems(Collections.emptyList());

        ExternalOrderDTO expectedResponse = new ExternalOrderDTO();
        expectedResponse.setOrderNumber("ORD-TEST-123");
        expectedResponse.setStatus("RECEIVED");
        expectedResponse.setTotalAmount(new BigDecimal("300.00"));
        expectedResponse.setItems(Collections.emptyList());

        when(externalProductAClient.submitOrder(any(ExternalOrderDTO.class))).thenReturn(expectedResponse);

        ExternalOrderDTO result = externalProductAClient.submitOrder(orderRequest);

        assertNotNull(result);
        assertEquals("ORD-TEST-123", result.getOrderNumber());
        assertEquals("RECEIVED", result.getStatus());
        assertEquals(new BigDecimal("300.00"), result.getTotalAmount());

        verify(externalProductAClient).submitOrder(orderRequest);
    }
}