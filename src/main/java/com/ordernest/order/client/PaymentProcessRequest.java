package com.ordernest.order.client;

import java.util.UUID;

public record PaymentProcessRequest(
        UUID orderId
) {
}
