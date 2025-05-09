package com.orderservice.util;

import com.orderservice.dto.OrderItemDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OrderCalculator {

    /**
     * Calcula o valor total de uma lista de itens de pedido
     */
    public BigDecimal calculateTotal(List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .map(item -> calculateItemTotal(item).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula o valor total de um item (preço * quantidade)
     */
    public BigDecimal calculateItemTotal(OrderItemDTO item) {
        if (item.getPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }

        return item.getPrice().multiply(new BigDecimal(item.getQuantity()));
    }
}