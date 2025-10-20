package com.streamcart.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request payload for creating a new order")
public record CreateOrderRequest(
    @Schema(description = "List of items in the order. Must contain at least one item.",
            example = "[{\"productId\":\"DM-PAPER-001\",\"productName\":\"Dunder Mifflin Paper\",\"quantity\":100,\"price\":6.99}]")
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<OrderItemRequest> items
) {
    
    @Schema(description = "Individual item in an order")
    public record OrderItemRequest(
        @Schema(description = "Unique product identifier from the catalog",
                example = "DM-PAPER-001")
        @NotBlank(message = "Product ID is required")
        String productId,
        
        @Schema(description = "Name of the product",
                example = "Dunder Mifflin Paper - Premium White")
        @NotBlank(message = "Product name is required")
        String productName,
        
        @Schema(description = "Quantity of the product to order",
                example = "100")
        Integer quantity,
        
        @Schema(description = "Price per unit of the product",
                example = "6.99")
        BigDecimal price
    ) {}
}

