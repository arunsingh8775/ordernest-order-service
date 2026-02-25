package com.ordernest.order.dto;

import com.ordernest.order.entity.OrderStatus;
import com.ordernest.order.entity.PaymentStatus;
import java.util.UUID;

public record PayOrderResponse(
        UUID orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String paymentId,
        String reason
) {
}
