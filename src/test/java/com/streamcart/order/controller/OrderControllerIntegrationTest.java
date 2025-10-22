package com.streamcart.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamcart.order.dto.CreateOrderRequest;
import com.streamcart.order.entity.Order;
import com.streamcart.order.entity.OrderStatus;
import com.streamcart.order.entity.User;
import com.streamcart.order.publisher.OrderEventPublisher;
import com.streamcart.order.repository.OrderRepository;
import com.streamcart.order.repository.UserRepository;
import com.streamcart.order.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController
 * Tests full HTTP request/response cycle with real Spring context, database, and JWT authentication
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback database changes after each test
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private OrderEventPublisher orderEventPublisher;  // Mock Kafka publisher for tests

    private String validJwtToken;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        // Clean up existing test data
        orderRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .firstName("Test")
                .lastName("User")
                .build();
        testUser = userRepository.save(testUser);

        // Create another user for authorization tests
        otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .password(passwordEncoder.encode("password"))
                .firstName("Other")
                .lastName("User")
                .build();
        otherUser = userRepository.save(otherUser);

        // Generate JWT for test user
        validJwtToken = jwtUtil.generateToken("testuser");
    }

    // ========== CREATE ORDER TESTS ==========

    @Test
    void testCreateOrder_WithValidJwtAndData_ReturnsCreatedStatus() throws Exception {
        // Arrange
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest(
                "PROD-001",
                "Test Product",
                2,
                new BigDecimal("10.00")
        );
        CreateOrderRequest request = new CreateOrderRequest(List.of(item));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.totalAmount").value(20.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testCreateOrder_WithMultipleItems_CalculatesTotalCorrectly() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.OrderItemRequest("PROD-001", "Product 1", 2, new BigDecimal("10.00")),
                new CreateOrderRequest.OrderItemRequest("PROD-002", "Product 2", 1, new BigDecimal("15.50")),
                new CreateOrderRequest.OrderItemRequest("PROD-003", "Product 3", 3, new BigDecimal("5.00"))
        ));

        // Act & Assert - Total should be (2*10.00) + (1*15.50) + (3*5.00) = 50.50
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(50.50));
    }

    @Test
    void testCreateOrder_WithoutJwtToken_ReturnsForbidden() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.OrderItemRequest("PROD-001", "Test Product", 1, new BigDecimal("10.00"))
        ));

        // Act & Assert - Spring Security 6.x returns 403 for anonymous users
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateOrder_WithInvalidJwtToken_ReturnsForbidden() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.OrderItemRequest("PROD-001", "Test Product", 1, new BigDecimal("10.00"))
        ));

        // Act & Assert - Invalid JWT is treated as anonymous, returns 403
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateOrder_WithEmptyItemsList_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(List.of());

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_WithInvalidItemData_ReturnsBadRequest() throws Exception {
        // Arrange - Missing required fields in JSON
        String invalidJson = """
            {
                "items": [
                    {
                        "productId": "",
                        "productName": "",
                        "quantity": 0,
                        "price": -10.00
                    }
                ]
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ========== GET ORDER TESTS ==========

    @Test
    void testGetOrder_WithValidJwtAndOwnOrder_ReturnsOrder() throws Exception {
        // Arrange - Create an order for testUser
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUser(testUser);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("25.00"));
        order = orderRepository.save(order);

        // Act & Assert
        mockMvc.perform(get("/api/orders/{orderId}", order.getOrderId())
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetOrder_WithoutJwtToken_ReturnsForbidden() throws Exception {
        // Arrange
        String orderId = "some-order-id";

        // Act & Assert - No JWT means anonymous, returns 403
        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetOrder_WithNonExistentOrderId_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentOrderId = "non-existent-order-id";

        // Act & Assert
        mockMvc.perform(get("/api/orders/{orderId}", nonExistentOrderId)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetOrder_WithOtherUsersOrder_ReturnsForbidden() throws Exception {
        // Arrange - Create an order for otherUser
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUser(otherUser);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("30.00"));
        order = orderRepository.save(order);

        // Act & Assert - testUser tries to access otherUser's order
        mockMvc.perform(get("/api/orders/{orderId}", order.getOrderId())
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isForbidden());
    }

    // ========== GET MY ORDERS TESTS ==========

    @Test
    void testGetMyOrders_WithValidJwt_ReturnsUserOrders() throws Exception {
        // Arrange - Create multiple orders for testUser
        Order order1 = new Order();
        order1.setOrderId(UUID.randomUUID().toString());
        order1.setUser(testUser);
        order1.setStatus(OrderStatus.PENDING);
        order1.setTotalAmount(new BigDecimal("10.00"));
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setOrderId(UUID.randomUUID().toString());
        order2.setUser(testUser);
        order2.setStatus(OrderStatus.PENDING);
        order2.setTotalAmount(new BigDecimal("20.00"));
        orderRepository.save(order2);

        // Create an order for otherUser (should not be returned)
        Order otherOrder = new Order();
        otherOrder.setOrderId(UUID.randomUUID().toString());
        otherOrder.setUser(otherUser);
        otherOrder.setStatus(OrderStatus.PENDING);
        otherOrder.setTotalAmount(new BigDecimal("99.99"));
        orderRepository.save(otherOrder);

        // Act & Assert
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username", everyItem(is("testuser"))))
                .andExpect(jsonPath("$[*].totalAmount", containsInAnyOrder(10.00, 20.00)));
    }

    @Test
    void testGetMyOrders_WithNoOrders_ReturnsEmptyList() throws Exception {
        // Act & Assert - testUser has no orders
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetMyOrders_WithoutJwtToken_ReturnsForbidden() throws Exception {
        // Act & Assert - Anonymous access returns 403
        mockMvc.perform(get("/api/orders/my-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetMyOrders_WithInvalidJwtToken_ReturnsForbidden() throws Exception {
        // Arrange - Malformed token is treated as anonymous
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MH0.invalid";

        // Act & Assert - Invalid token treated as anonymous, returns 403
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden());
    }

    // ========== AUTHORIZATION TESTS ==========

    @Test
    void testOrderEndpoints_EnforceAuthenticationOnAllRoutes() throws Exception {
        // Test that all order endpoints require authentication (returns 403 for anonymous)
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.OrderItemRequest("PROD-001", "Test", 1, new BigDecimal("10.00"))
        ));

        // POST /api/orders
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // GET /api/orders/{orderId}
        mockMvc.perform(get("/api/orders/some-id"))
                .andExpect(status().isForbidden());

        // GET /api/orders/my-orders
        mockMvc.perform(get("/api/orders/my-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOrderCreation_ExtractsUsernameFromJwtNotRequestBody() throws Exception {
        // This test verifies that the username comes from JWT, not user input
        // Even if we somehow passed a different username in the request (we don't),
        // the order should be created for the JWT user

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.OrderItemRequest("PROD-001", "Test Product", 1, new BigDecimal("10.00"))
        ));

        // Act & Assert - Order should be created for "testuser" from JWT
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser")); // From JWT, not manipulable
    }
}

