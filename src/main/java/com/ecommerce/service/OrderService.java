package com.ecommerce.service;

import com.ecommerce.dto.request.PlaceOrderRequest;
import com.ecommerce.dto.response.OrderDto;
import com.ecommerce.dto.response.OrderItemDto;
import com.ecommerce.entity.*;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.exception.OutOfStockException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional
    public OrderDto placeOrder(Long userId, PlaceOrderRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PlaceOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepo.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new OutOfStockException(
                        product.getName(), product.getStockQuantity(), itemReq.getQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepo.save(product);

            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            items.add(OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        Order order = orderRepo.save(Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build());

        items.forEach(item -> item.setOrder(order));
        List<OrderItem> savedItems = orderItemRepo.saveAll(items);

        return toOrderDto(order, savedItems);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        List<OrderItem> items = orderItemRepo.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepo.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepo.save(order);

        return toOrderDto(saved, items);
    }

    public List<OrderDto> getOrdersByUser(Long userId) {
        return orderRepo.findByUserId(userId).stream()
                .map(order -> toOrderDto(order, orderItemRepo.findByOrderId(order.getId())))
                .toList();
    }

    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return toOrderDto(order, orderItemRepo.findByOrderId(orderId));
    }

    private OrderDto toOrderDto(Order order, List<OrderItem> items) {
        return new OrderDto(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getTotalAmount(),
                items.stream().map(this::toOrderItemDto).toList(),
                order.getCreatedAt()
        );
    }

    private OrderItemDto toOrderItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}