package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.*;
import com.project.ecommerce_backend.entity.*;
import com.project.ecommerce_backend.repository.OrderRepository;
import com.project.ecommerce_backend.repository.PaymentRepository;
import com.project.ecommerce_backend.repository.ProductRepository;
import com.project.ecommerce_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private CartService cartService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;   // added

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrder_WhenCartHasValidItems_ShouldCreateOrderAndDeductStock() {
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 2;
        BigDecimal price = new BigDecimal("99.99");

        User user = new User();
        user.setId(userId);

        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setPrice(price);
        product.setStockQuantity(5);

        Cart cart = new Cart(userId, new ArrayList<>());
        cart.getItems().add(new CartItem(productId, quantity));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderResponse response = orderService.createOrder(userId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(price.multiply(BigDecimal.valueOf(quantity)));
        assertThat(response.getItems()).hasSize(1);
        assertThat(product.getStockQuantity()).isEqualTo(3);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartService, times(1)).clearCart(userId);
    }

    @Test
    void createOrder_WhenProductNotFound_ShouldThrowException() {
        Long userId = 1L;
        Long productId = 999L;

        User user = new User();
        user.setId(userId);

        Cart cart = new Cart(userId, new ArrayList<>());
        cart.getItems().add(new CartItem(productId, 1));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_WhenInsufficientStock_ShouldThrowException() {
        Long userId = 1L;
        Long productId = 10L;
        int requestedQuantity = 10;

        User user = new User();
        user.setId(userId);

        Product product = new Product();
        product.setId(productId);
        product.setStockQuantity(3);

        Cart cart = new Cart(userId, new ArrayList<>());
        cart.getItems().add(new CartItem(productId, requestedQuantity));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersForUser_ShouldReturnListOfOrders() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setTotalAmount(new BigDecimal("199.98"));
        order1.setStatus(OrderStatus.PENDING);
        order1.setUser(user);

        Product product = new Product();
        product.setId(10L);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProduct(product);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("99.99"));
        order1.addItem(item);

        when(orderRepository.findByUserId(userId)).thenReturn(List.of(order1));

        List<OrderResponse> responses = orderService.getOrdersForUser(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getItems()).hasSize(1);
        verify(orderRepository, times(1)).findByUserId(userId);
    }

    @Test
    void processPayment_WhenOrderExistAndGatewaySuccess_ShouldUpdateOrderAndCreatePayment() {
        //Arrange

        Long userId = 1L;
        Long orderId = 100L;
        User user = new User();
        user.setId(userId);

        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(eq(orderId), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(true, "TXN-123"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        //Act
        OrderResponse response = orderService.processPayment(userId, orderId);

        //Assert
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(order);


    }

    @Test
    void processPayment_WhenGatewayFails_ShouldUpdateOrderToFailed() {
        //Arrange

        Long userId = 1L;
        Long orderId = 100L;
        User user = new User();
        user.setId(userId);

        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(eq(orderId), any(BigDecimal.class)))
                .thenReturn(new PaymentResult(false, null));


      //Act & Assert
        assertThatThrownBy(() -> orderService.processPayment(userId, orderId))
                .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Payment failed");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));

    }
}