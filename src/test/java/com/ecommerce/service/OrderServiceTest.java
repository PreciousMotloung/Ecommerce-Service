package com.ecommerce.service;

import com.ecommerce.dto.request.PlaceOrderRequest;
import com.ecommerce.dto.response.OrderDto;
import com.ecommerce.entity.*;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.exception.OutOfStockException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private OrderItemRepository orderItemRepo;
    @Mock private ProductRepository productRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("john").email("john@example.com")
                .role(Role.USER).createdAt(LocalDateTime.now()).build();

        product = Product.builder()
                .id(1L).name("Laptop").description("A laptop")
                .price(new BigDecimal("999.99")).stockQuantity(10)
                .createdAt(LocalDateTime.now()).build();

        order = Order.builder()
                .id(1L).user(user).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .createdAt(LocalDateTime.now()).build();

        orderItem = OrderItem.builder()
                .id(1L).order(order).product(product)
                .quantity(2).unitPrice(new BigDecimal("999.99")).build();
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_createsOrderSuccessfully() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(1L, 2)));

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(orderRepo.save(any(Order.class))).thenReturn(order);
        when(orderItemRepo.saveAll(any())).thenReturn(List.of(orderItem));

        OrderDto result = orderService.placeOrder(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("john");
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("1999.98");
        assertThat(result.items()).hasSize(1);
        verify(productRepo).save(any(Product.class));
        verify(orderRepo).save(any(Order.class));
        verify(orderItemRepo).saveAll(any());
    }

    @Test
    void placeOrder_throwsUserNotFoundException_whenUserNotFound() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(1L, 1)));
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(99L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void placeOrder_throwsProductNotFoundException_whenProductNotFound() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(99L, 1)));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(1L, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void placeOrder_throwsOutOfStockException_whenStockInsufficient() {
        product.setStockQuantity(1);
        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new PlaceOrderRequest.OrderItemRequest(1L, 5)));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrder(1L, request))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("Laptop")
                .hasMessageContaining("requested 5")
                .hasMessageContaining("only 1 available");
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    void cancelOrder_cancelsOrderAndRestoresStock() {
        Order cancelled = Order.builder()
                .id(1L).user(user).status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("1999.98"))
                .createdAt(order.getCreatedAt()).build();

        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepo.findByOrderId(1L)).thenReturn(List.of(orderItem));
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(orderRepo.save(any(Order.class))).thenReturn(cancelled);

        OrderDto result = orderService.cancelOrder(1L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(productRepo).save(any(Product.class));
        verify(orderRepo).save(any(Order.class));
    }

    @Test
    void cancelOrder_throwsOrderNotFoundException_whenOrderNotFound() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void cancelOrder_throwsIllegalStateException_whenOrderAlreadyCancelled() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    // ── getOrdersByUser ───────────────────────────────────────────────────────

    @Test
    void getOrdersByUser_returnsListOfOrderDtos() {
        when(orderRepo.findByUserId(1L)).thenReturn(List.of(order));
        when(orderItemRepo.findByOrderId(1L)).thenReturn(List.of(orderItem));

        List<OrderDto> result = orderService.getOrdersByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        assertThat(result.get(0).items()).hasSize(1);
    }

    @Test
    void getOrdersByUser_returnsEmptyList_whenUserHasNoOrders() {
        when(orderRepo.findByUserId(1L)).thenReturn(List.of());

        List<OrderDto> result = orderService.getOrdersByUser(1L);

        assertThat(result).isEmpty();
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    void getOrderById_returnsOrderDto_whenFound() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepo.findByOrderId(1L)).thenReturn(List.of(orderItem));

        OrderDto result = orderService.getOrderById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.items()).hasSize(1);

        // verify subtotal is computed: 999.99 × 2 = 1999.98
        assertThat(result.items().get(0).subtotal()).isEqualByComparingTo("1999.98");
    }

    @Test
    void getOrderById_throwsOrderNotFoundException_whenNotFound() {
        when(orderRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }
}