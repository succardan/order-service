package com.orderservice.dto;

import com.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private UUID id;
    private String orderNumber;

    @NotEmpty(message = "A lista de itens não pode estar vazia")
    @Valid
    private List<OrderItemDTO> items;

    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private BigDecimal totalAmount;
}