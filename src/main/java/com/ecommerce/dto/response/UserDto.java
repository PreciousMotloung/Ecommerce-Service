package com.ecommerce.dto.response;

import com.ecommerce.entity.Role;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role,
        LocalDateTime createdAt
) {}