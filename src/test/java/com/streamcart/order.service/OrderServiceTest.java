package com.streamcart.order.service;

import com.streamcart.order.dto.CreateOrderRequest;
import com.streamcart.order.dto.OrderResponse;
import com.streamcart.order.entity.Order;
import com.streamcart.order.entity.OrderStatus;
import com.streamcart.order.entity.User;
import com.streamcart.order.exception.UserNotFoundException;
import com.streamcart.order.publisher.OrderEventPublisher;
import com.streamcart.order.repository.OrderRepository;
import com.streamcart.order.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set up authentication context
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("testuser", null, List.of())
        );

        // Create test user
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("hashedpassword")
            .build();
    }

    @Test
    void testCreateOrder_Success() {
        // Arrange
        CreateOrderRequest.OrderItemRequest itemRequest = 
            new CreateOrderRequest.OrderItemRequest(
                "PROD-001", 
                "Test Product", 
                2, 
                new BigDecimal("10.00")
            );
        CreateOrderRequest request = new CreateOrderRequest(List.of(itemRequest));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return order;
        });

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.totalAmount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);

        // Verify interactions
        verify(userRepository).findByUsername("testuser");
        verify(orderRepository).save(any(Order.class));
        verify(orderEventPublisher).publishOrderCreated(any());
    }

    @Test
    void testCreateOrder_UserNotFound() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(List.of());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("testuser");

        verify(orderRepository, never()).save(any());
        verify(orderEventPublisher, never()).publishOrderCreated(any());
    }
    @Test 
    void testGetOrder_Success() {
        // Arrange
        String orderId = "1234567890";
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUser(testUser);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.setCreatedAt(LocalDateTime.now());
        
        // Only mock what's actually called
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        // Act
        OrderResponse response = orderService.getOrder(orderId);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.totalAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        
        // Verify only what's actually called
        verify(orderRepository).findById(orderId);
        // That's it! userRepository and findByUser are NOT called by getOrder()
    }
    @Test
    void testGetMyOrders_Success() {
        // Arrange
        List<Order> orders = new ArrayList<>();
        Order order1 = new Order();
        order1.setOrderId("1234567890");
        order1.setUser(testUser);
        order1.setStatus(OrderStatus.PENDING);
        order1.setTotalAmount(new BigDecimal("10.00"));
        order1.setCreatedAt(LocalDateTime.now());
        orders.add(order1);
        Order order2 = new Order();
        order2.setOrderId("1234567891");
        order2.setUser(testUser);
        order2.setStatus(OrderStatus.PENDING);
        order2.setTotalAmount(new BigDecimal("20.00"));
        order2.setCreatedAt(LocalDateTime.now());
        orders.add(order2);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser)).thenReturn(orders);

        // Act
        List<OrderResponse> response = orderService.getMyOrders();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get(0).orderId()).isEqualTo("1234567890");
        assertThat(response.get(0).username()).isEqualTo("testuser");
        assertThat(response.get(0).totalAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(response.get(0).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.get(1).orderId()).isEqualTo("1234567891");
        assertThat(response.get(1).username()).isEqualTo("testuser");
        assertThat(response.get(1).totalAmount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(response.get(1).status()).isEqualTo(OrderStatus.PENDING);

        // Verify only what's actually called
        verify(userRepository).findByUsername("testuser");
        verify(orderRepository).findByUser(testUser);
    }
}
