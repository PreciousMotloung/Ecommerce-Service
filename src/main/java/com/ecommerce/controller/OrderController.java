package com.ecommerce.controller;

import com.ecommerce.dto.request.PlaceOrderRequest;
import com.ecommerce.dto.response.ErrorResponse;
import com.ecommerce.dto.response.OrderDto;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management — USER role required")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a new order",
            description = "Deducts stock and creates the order in a single transaction",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order placed"),
                    @ApiResponse(responseCode = "409", description = "Insufficient stock",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public OrderDto placeOrder(@Valid @RequestBody PlaceOrderRequest request,
                               Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        return orderService.placeOrder(userId, request);
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get all orders for the authenticated user",
            responses = @ApiResponse(responseCode = "200", description = "Orders retrieved"))
    public List<OrderDto> getMyOrders(Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        return orderService.getOrdersByUser(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID — own orders only",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order found"),
                    @ApiResponse(responseCode = "403", description = "Not your order",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Order not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public OrderDto getOrderById(@PathVariable Long id, Authentication authentication) {
        OrderDto order = orderService.getOrderById(id);
        assertOwnership(order, authentication);
        return order;
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order — own orders only",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order cancelled"),
                    @ApiResponse(responseCode = "403", description = "Not your order",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Order already cancelled",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public OrderDto cancelOrder(@PathVariable Long id, Authentication authentication) {
        OrderDto order = orderService.getOrderById(id);
        assertOwnership(order, authentication);
        return orderService.cancelOrder(id);
    }

    private void assertOwnership(OrderDto order, Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        if (!order.userId().equals(userId)) {
            throw new AccessDeniedException("You can only access your own orders");
        }
    }
}