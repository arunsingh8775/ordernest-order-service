package com.ordernest.order.dto;

import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        Integer quantity
) {
}
