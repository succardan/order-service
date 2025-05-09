package com.orderservice.controller;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderStatusDTO;
import com.orderservice.exception.DuplicateOrderException;
import com.orderservice.model.OrderStatus;
import com.orderservice.service.OrderService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @RateLimiter(name = "default")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody @Validated OrderDTO orderDTO) {
        try {
            OrderDTO createdOrder = orderService.createOrder(orderDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (DuplicateOrderException e) {
            log.warn("Tentativa de criar pedido duplicado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable UUID id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDTO> getOrderByNumber(@PathVariable String orderNumber) {
        OrderDTO order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatusDTO> getOrderStatus(@PathVariable UUID id) {
        OrderStatusDTO status = orderService.getOrderStatus(id);
        return ResponseEntity.ok(status);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {

        List<OrderDTO> orders = orderService.listOrders(page, size, status);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<OrderDTO> processOrder(@PathVariable UUID id) {
        OrderDTO processedOrder = orderService.processOrder(id);
        return ResponseEntity.ok(processedOrder);
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<OrderDTO> notifyExternalSystem(@PathVariable UUID id) {
        OrderDTO notifiedOrder = orderService.notifyExternalSystem(id);
        return ResponseEntity.ok(notifiedOrder);
    }
}