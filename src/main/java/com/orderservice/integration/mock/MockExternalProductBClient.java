package com.orderservice.integration.mock;

import com.orderservice.integration.ExternalProductBClient;
import com.orderservice.integration.dto.ExternalOrderDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Profile("mock")
public class MockExternalProductBClient implements ExternalProductBClient {

    private final Map<String, String> orderStatusDatabase = new HashMap<>();

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "externalServiceB", fallbackMethod = "getOrderStatusFallback")
    public String getOrderStatus(String orderNumber) {
        simulateNetworkDelay();
        
        return orderStatusDatabase.getOrDefault(orderNumber, "UNKNOWN");
    }

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "externalServiceB", fallbackMethod = "notifyOrderFallback")
    public void notifyOrder(ExternalOrderDTO orderDTO) {
        simulateNetworkDelay();
        
        String status = "NOTIFIED";
        orderStatusDatabase.put(orderDTO.getOrderNumber(), status);
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getOrderStatusFallback(String orderNumber, Exception ex) {
        return "ERROR";
    }

    public void notifyOrderFallback(ExternalOrderDTO orderDTO, Exception ex) {
    }
}
