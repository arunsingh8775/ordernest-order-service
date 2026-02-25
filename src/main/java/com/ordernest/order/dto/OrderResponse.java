package com.ordernest.order.dto;

import com.ordernest.order.entity.OrderStatus;
import com.ordernest.order.entity.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderItemResponse item,
        OrderStatus status,
        PaymentStatus paymentStatus,
        Instant createdAt
) {
}
