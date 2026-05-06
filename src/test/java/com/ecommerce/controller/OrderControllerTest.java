package com.ecommerce.controller;

import com.ecommerce.dto.request.PlaceOrderRequest;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.security.JwtUtil;
import com.ecommerce.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserDetailsServiceImpl userDetailsService;

    private User user1;
    private User user2;
    private Product product;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .username("orderuser1").email("orderuser1@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        user2 = userRepository.save(User.builder()
                .username("orderuser2").email("orderuser2@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        user1Token = jwtUtil.generateToken(userDetailsService.loadUserByUsername("orderuser1@test.com"));
        user2Token = jwtUtil.generateToken(userDetailsService.loadUserByUsername("orderuser2@test.com"));

        Category cat = categoryRepository.save(Category.builder()
                .name("OrderCat").description("Test category").build());

        product = productRepository.save(Product.builder()
                .name("Widget").description("A test widget")
                .price(new BigDecimal("50.00")).stockQuantity(10)
                .category(cat).build());
    }

    @AfterEach
    void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void placeOrder_withoutAuth_returns401() throws Exception {
        PlaceOrderRequest req = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(product.getId(), 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_withUserToken_returns201() throws Exception {
        PlaceOrderRequest req = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(product.getId(), 2)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void placeOrder_outOfStock_returns409() throws Exception {
        PlaceOrderRequest req = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(product.getId(), 99)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Out of Stock"));
    }

    @Test
    void getMyOrders_withUserToken_returns200() throws Exception {
        // Place an order first
        PlaceOrderRequest req = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(product.getId(), 1)));
        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getOrderById_ownOrder_returns200() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .user(user1).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00")).build());

        mockMvc.perform(get("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()));
    }

    @Test
    void getOrderById_otherUsersOrder_returns403() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .user(user1).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00")).build());

        mockMvc.perform(get("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelOrder_ownOrder_returns200() throws Exception {
        // Place then cancel via HTTP to test full flow
        PlaceOrderRequest req = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(product.getId(), 1)));

        String placeResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(placeResult).get("id").asLong();

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_otherUsersOrder_returns403() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .user(user1).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00")).build());

        mockMvc.perform(put("/api/orders/{id}/cancel", order.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
}