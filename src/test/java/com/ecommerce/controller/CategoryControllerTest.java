package com.ecommerce.controller;

import com.ecommerce.dto.request.CreateCategoryRequest;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CategoryRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserDetailsServiceImpl userDetailsService;

    private String userToken;
    private String adminToken;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username("catuser").email("catuser@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        userRepository.save(User.builder()
                .username("catadmin").email("catadmin@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN).build());

        userToken = jwtUtil.generateToken(userDetailsService.loadUserByUsername("catuser@test.com"));
        adminToken = jwtUtil.generateToken(userDetailsService.loadUserByUsername("catadmin@test.com"));

        Category cat = categoryRepository.save(Category.builder()
                .name("Electronics").description("Electronic goods").build());
        categoryId = cat.getId();
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getAllCategories_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCategories_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getCategoryById_withUserToken_returns200() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getCategoryById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/categories/9999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Category Not Found"));
    }

    @Test
    void createCategory_withUserToken_returns403() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("Books", "All books");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_withAdminToken_returns201() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("Books", "All books");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Books"));
    }

    @Test
    void createCategory_invalidBody_returns400() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("", null);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}