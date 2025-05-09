package com.orderservice.integration.mock;

import com.orderservice.integration.ExternalProductAClient;
import com.orderservice.integration.dto.ExternalOrderDTO;
import com.orderservice.integration.dto.ExternalProductDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("mock")
public class MockExternalProductAClient implements ExternalProductAClient {

    private final Map<String, ExternalProductDTO> productDatabase = new HashMap<>();
    private final Map<String, ExternalOrderDTO> orderDatabase = new HashMap<>();

    public MockExternalProductAClient() {
        initializeProducts();
    }

    private void initializeProducts() {
        for (int i = 1; i <= 10; i++) {
            String productId = "PROD-" + i;
            ExternalProductDTO product = ExternalProductDTO.builder()
                .id(productId)
                .name("Produto " + i)
                .price(BigDecimal.valueOf(10.0 * i))
                .available(true)
                .build();
            productDatabase.put(productId, product);
        }
    }

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "externalServiceA", fallbackMethod = "getProductFallback")
    public ExternalProductDTO getProduct(String productId) {
        simulateNetworkDelay();
        
        return productDatabase.getOrDefault(productId, createDefaultProduct(productId));
    }

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "externalServiceA", fallbackMethod = "getProductsFallback")
    public List<ExternalProductDTO> getProducts() {
        simulateNetworkDelay();
        
        return new ArrayList<>(productDatabase.values());
    }

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "externalServiceA", fallbackMethod = "submitOrderFallback")
    public ExternalOrderDTO submitOrder(ExternalOrderDTO orderDTO) {
        simulateNetworkDelay();
        
        String confirmationId = UUID.randomUUID().toString();
        orderDTO.setConfirmationId(confirmationId);
        orderDTO.setStatus("CONFIRMED");
        
        orderDatabase.put(confirmationId, orderDTO);
        
        return orderDTO;
    }

    private ExternalProductDTO createDefaultProduct(String productId) {
        return ExternalProductDTO.builder()
            .id(productId)
            .name("Produto Desconhecido")
            .price(BigDecimal.valueOf(99.99))
            .available(false)
            .build();
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public ExternalProductDTO getProductFallback(String productId, Exception ex) {
        return createDefaultProduct(productId);
    }

    public List<ExternalProductDTO> getProductsFallback(Exception ex) {
        return new ArrayList<>();
    }

    public ExternalOrderDTO submitOrderFallback(ExternalOrderDTO orderDTO, Exception ex) {
        ExternalOrderDTO fallbackOrder = new ExternalOrderDTO();
        fallbackOrder.setStatus("ERROR");
        return fallbackOrder;
    }
}
