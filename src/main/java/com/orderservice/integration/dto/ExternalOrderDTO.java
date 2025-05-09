package com.orderservice.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalOrderDTO {

    private String orderNumber;
    private String status;
    private List<ExternalOrderItemDTO> items;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalOrderItemDTO {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}