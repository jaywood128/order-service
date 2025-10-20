package com.streamcart.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.streamcart.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing order information")
public record OrderResponse(
    @Schema(description = "Unique identifier for the order",
            example = "550e8400-e29b-41d4-a716-446655440000")
    String orderId,
    
    @Schema(description = "Username of the user who placed the order",
            example = "mscott")
    String username,
    
    @Schema(description = "Total amount for the order in USD",
            example = "699.00")
    BigDecimal totalAmount,
    
    @Schema(description = "Current status of the order",
            example = "PENDING",
            allowableValues = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"})
    OrderStatus status,
    
    @Schema(description = "Timestamp when the order was created",
            example = "2025-10-20T14:30:00")
    LocalDateTime createdAt
) {
}
