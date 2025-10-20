package com.streamcart.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
    String orderId,
    String username,
    BigDecimal totalAmount,
    List<OrderItemDto> items,
    LocalDateTime timestamp
) {
    public record OrderItemDto(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal price
    ) {}
}
