package com.ordernest.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID userId,
        @Valid @NotNull ItemRequest item
) {
    public record ItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
