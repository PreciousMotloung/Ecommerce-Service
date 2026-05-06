package com.ecommerce.controller;

import com.ecommerce.dto.request.CreateProductRequest;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserDetailsServiceImpl userDetailsService;

    private String userToken;
    private String adminToken;
    private Long productId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username("produser").email("produser@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        userRepository.save(User.builder()
                .username("prodadmin").email("prodadmin@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN).build());

        userToken = jwtUtil.generateToken(userDetailsService.loadUserByUsername("produser@test.com"));
        adminToken = jwtUtil.generateToken(userDetailsService.loadUserByUsername("prodadmin@test.com"));

        Category cat = categoryRepository.save(Category.builder()
                .name("Gadgets").description("Cool gadgets").build());
        categoryId = cat.getId();

        Product product = productRepository.save(Product.builder()
                .name("Laptop").description("A powerful laptop")
                .price(new BigDecimal("999.99")).stockQuantity(10)
                .category(cat).build());
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getAllProducts_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllProducts_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void getProductById_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getProductsByCategory_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/api/products/category/{id}", categoryId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(categoryId));
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/9999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product Not Found"));
    }

    @Test
    void createProduct_withUserToken_returns403() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Phone", "A phone", new BigDecimal("499.99"), 5, null);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withAdminToken_returns201() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "Tablet", "A tablet", new BigDecimal("299.99"), 15, null);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tablet"))
                .andExpect(jsonPath("$.stockQuantity").value(15));
    }

    @Test
    void createProduct_invalidBody_returns400() throws Exception {
        CreateProductRequest req = new CreateProductRequest("", null, null, -1, null);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}