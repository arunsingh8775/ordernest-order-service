package com.ordernest.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount
) {
}
