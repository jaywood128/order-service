package com.streamcart.order.service;

import com.streamcart.order.dto.CreateOrderRequest;
import com.streamcart.order.dto.OrderCreatedEvent;
import com.streamcart.order.dto.OrderResponse;
import com.streamcart.order.entity.Order;
import com.streamcart.order.entity.OrderItem;
import com.streamcart.order.entity.OrderStatus;
import com.streamcart.order.entity.User;
import com.streamcart.order.exception.AccessDeniedException;
import com.streamcart.order.exception.OrderNotFoundException;
import com.streamcart.order.exception.UnauthorizedException;
import com.streamcart.order.exception.UserNotFoundException;
import com.streamcart.order.publisher.OrderEventPublisher;
import com.streamcart.order.repository.OrderRepository;
import com.streamcart.order.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final UserRepository userRepository;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Get current authenticated user from JWT
        String username = getCurrentUsername();
        log.info("Creating order for user: {}", username);
        
        // Load user from database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));
        
        // Create order entity
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        
        // Add order items
        for (CreateOrderRequest.OrderItemRequest itemReq : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemReq.productId());
            item.setProductName(itemReq.productName());
            item.setQuantity(itemReq.quantity());
            item.setPrice(itemReq.price());
            order.addItem(item);
        }
        
        // Calculate total
        BigDecimal total = order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        
        // Save to database
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved to database: {}", savedOrder.getOrderId());
        
        // Publish event to Kafka
        OrderCreatedEvent event = mapToEvent(savedOrder);
        eventPublisher.publishOrderCreated(event);
        
        return mapToResponse(savedOrder);
    }
    
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Verify order belongs to current user
        String currentUsername = getCurrentUsername();
        if (!order.getUser().getUsername().equals(currentUsername)) {
            log.warn("User {} attempted to access order {} belonging to {}", 
                currentUsername, orderId, order.getUser().getUsername());
            throw new AccessDeniedException("Access denied: Order does not belong to current user");
        }
        
        return mapToResponse(order);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        String username = getCurrentUsername();
        log.info("Fetching orders for user: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));
        
        return orderRepository.findByUser(user).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return authentication.getName();
    }
    
    private OrderCreatedEvent mapToEvent(Order order) {
        List<OrderCreatedEvent.OrderItemDto> items = order.getItems().stream()
            .map(item -> new OrderCreatedEvent.OrderItemDto(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
            ))
            .collect(Collectors.toList());
        
        return new OrderCreatedEvent(
            order.getOrderId(),
            order.getUser().getUsername(),
            order.getTotalAmount(),
            items,
            LocalDateTime.now()
        );
    }
    
    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getUser().getUsername(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
