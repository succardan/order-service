package com.orderservice.integration;

import com.orderservice.integration.dto.ExternalOrderDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "external-product-b", url = "${external-service.product-b.url}")
public interface ExternalProductBClient {

    @GetMapping("/orders/{orderNumber}")
    @CircuitBreaker(name = "externalServiceB", fallbackMethod = "getOrderStatusFallback")
    @RateLimiter(name = "default")
    String getOrderStatus(@PathVariable("orderNumber") String orderNumber);

    @PostMapping("/orders")
    @CircuitBreaker(name = "externalServiceB", fallbackMethod = "notifyOrderFallback")
    @RateLimiter(name = "default")
    void notifyOrder(@RequestBody ExternalOrderDTO orderDTO);
}