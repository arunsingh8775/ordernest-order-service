package com.ordernest.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryProductResponse(
        UUID id,
        String name,
        BigDecimal price,
        Integer availableQuantity,
        String currency
) {
}
