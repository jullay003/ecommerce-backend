package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.*;
import com.project.ecommerce_backend.entity.*;
import com.project.ecommerce_backend.repository.OrderRepository;
import com.project.ecommerce_backend.repository.ProductRepository;
import com.project.ecommerce_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderService(CartService cartService,
                        ProductRepository productRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse createOrder(Long userId) {
        // 1. Get the cart for this user
        Cart cart = cartService.getCart(userId);

        // 2. If cart is empty, throw an exception
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Prepare an Order entity
        Order order = new Order();
        order.setUser(user);   // minimal user reference, only ID needed for FK
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        // 4. Process each cart item: validate stock, create OrderItem, calculate total
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Check stock availability
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            // Create OrderItem and set its fields
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            // Add to order (this also sets the back-reference)
            order.addItem(orderItem);

            // Accumulate total amount
            order.setTotalAmount(order.getTotalAmount().add(
                    product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
            ));
        }

        // 5. Save the order (cascade saves order items automatically)
        Order savedOrder = orderRepository.save(order);

        // 6. Clear the cart after successful order creation
        cartService.clearCart(userId);

        // 7. Convert to response DTO and return
        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }
        return responses;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemResponses.add(new OrderItemResponse(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    item.getPrice()
            ));
        }
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus(),
                itemResponses
        );
    }
}