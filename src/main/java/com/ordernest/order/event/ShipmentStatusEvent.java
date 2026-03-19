package com.ordernest.order.event;

import com.ordernest.order.entity.ShipmentStatus;
import java.time.Instant;

public record ShipmentStatusEvent(
        String orderId,
        ShipmentStatus shipmentStatus,
        String updatedBy,
        Instant timestamp
) {
}
