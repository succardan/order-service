package com.orderservice.integration;

import com.orderservice.integration.dto.ExternalOrderDTO;
import com.orderservice.integration.dto.ExternalProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "external-product-a", url = "${external-service.product-a.url}")
public interface ExternalProductAClient {

    @GetMapping("/products/{productId}")
    @CircuitBreaker(name = "externalServiceA", fallbackMethod = "getProductFallback")
    @RateLimiter(name = "default")
    ExternalProductDTO getProduct(@PathVariable("productId") String productId);

    @GetMapping("/products")
    @CircuitBreaker(name = "externalServiceA", fallbackMethod = "getProductsFallback")
    @RateLimiter(name = "default")
    List<ExternalProductDTO> getProducts();

    @PostMapping("/orders")
    @CircuitBreaker(name = "externalServiceA", fallbackMethod = "submitOrderFallback")
    @RateLimiter(name = "default")
    ExternalOrderDTO submitOrder(@RequestBody ExternalOrderDTO orderDTO);
}