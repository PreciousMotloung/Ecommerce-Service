package com.ecommerce.controller;

import com.ecommerce.dto.request.LoginRequest;
import com.ecommerce.dto.request.RegisterRequest;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void register_returnsCreatedWithToken() throws Exception {
        RegisterRequest req = new RegisterRequest("alice", "alice@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        userRepository.save(User.builder()
                .username("bob").email("bob@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        RegisterRequest req = new RegisterRequest("bob2", "bob@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Email"));
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void login_returnsOkWithToken() throws Exception {
        userRepository.save(User.builder()
                .username("carol").email("carol@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        LoginRequest req = new LoginRequest("carol@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        userRepository.save(User.builder()
                .username("dan").email("dan@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER).build());

        LoginRequest req = new LoginRequest("dan@test.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonExistentEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest("nobody@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}