package com.orderservice.util;

import com.orderservice.dto.OrderDTO;
import com.orderservice.dto.OrderItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderValidator {

    public void validate(OrderDTO orderDTO) {
        validateItems(orderDTO.getItems());
        validateDuplicateProducts(orderDTO.getItems());
    }

    private void validateItems(List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Um pedido deve ter pelo menos um item");
        }

        items.forEach(item -> {
            if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos os itens devem ter um ID de produto válido");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("A quantidade de cada item deve ser maior que zero");
            }
        });
    }

    private void validateDuplicateProducts(List<OrderItemDTO> items) {
        Set<String> productIds = new HashSet<>();

        for (OrderItemDTO item : items) {
            if (!productIds.add(item.getProductId())) {
                throw new IllegalArgumentException("Produto duplicado no pedido: " + item.getProductId());
            }
        }
    }
}