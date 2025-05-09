package com.orderservice.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalProductDTO {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean available;
}