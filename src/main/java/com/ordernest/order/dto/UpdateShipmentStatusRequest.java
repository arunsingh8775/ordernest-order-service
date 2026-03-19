package com.ordernest.order.dto;

import com.ordernest.order.entity.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(
        @NotBlank(message = "orderId is required")
        String orderId,
        @NotNull(message = "shipmentStatus is required")
        ShipmentStatus shipmentStatus
) {
}
