package com.ecommerce.dto.response;

import java.time.LocalDateTime;

public record CategoryDto(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {}