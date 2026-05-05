package com.ecommerce.dto.response;

import com.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        Long userId,
        String username,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemDto> items,
        LocalDateTime createdAt
) {}