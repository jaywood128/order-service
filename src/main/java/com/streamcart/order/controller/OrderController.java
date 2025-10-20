package com.streamcart.order.controller;

import com.streamcart.order.dto.CreateOrderRequest;
import com.streamcart.order.dto.OrderResponse;
import com.streamcart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints. All operations require JWT authentication.")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    
    private final OrderService orderService;
    
    @Operation(
            summary = "Create a new order",
            description = "Creates a new order for the authenticated user. Order is automatically linked to the user from JWT token. " +
                    "Publishes an order.created event to Kafka for downstream processing by payment and inventory services."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully. Kafka event published.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request: validation errors or empty items list"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: missing or invalid JWT token"
            )
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request with {} items", request.items().size());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
            summary = "Get a specific order by ID",
            description = "Retrieves order details by order ID. Users can only access their own orders. " +
                    "Returns 403 Forbidden if trying to access another user's order."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order found and returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: missing or invalid JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: order does not belong to current user"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found"
            )
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Unique order identifier", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String orderId) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get all orders for authenticated user",
            description = "Retrieves all orders belonging to the currently authenticated user. " +
                    "User is identified from JWT token. Returns empty list if no orders found."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully (may be empty list)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: missing or invalid JWT token"
            )
    })
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        List<OrderResponse> orders = orderService.getMyOrders();
        return ResponseEntity.ok(orders);
    }
}
