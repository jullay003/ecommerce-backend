package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.Cart;
import com.project.ecommerce_backend.dto.CartItem;
import com.project.ecommerce_backend.entity.Product;
import com.project.ecommerce_backend.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;


    public CartService(StringRedisTemplate redisTemplate,
                       ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
        this.objectMapper = new ObjectMapper();
    }

    public Cart getCart(Long userId) {
        String key = CART_KEY_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return new Cart(userId, new java.util.ArrayList<>());
        }
        try {
            return objectMapper.readValue(json, Cart.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to read cart from Redis", e);
        }
    }

    public Cart addItem(Long userId, Long productId, int quantity) {
        if(quantity <= 0){
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = getCart(userId);
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if(existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            cart.getItems().add(new CartItem(productId, quantity));
        }
        saveCart(userId, cart);
        return cart;
    }

    public Cart updateItemQuantity(Long userId, Long productId, int quantity) {
        if(quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        Cart cart = getCart(userId);
        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> {
                            if(quantity == 0) {
                                cart.getItems().remove(item);
                            } else {
                                item.setQuantity(quantity);
                            }
                        },
                        () -> {throw new RuntimeException("Item not found in cart");}
                );
        saveCart(userId, cart);
        return cart;
    }

    public Cart removeItem(Long userId, Long productId) {
        Cart cart = getCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        saveCart(userId, cart);
        return cart;
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(CART_KEY_PREFIX + userId);
    }

    private void saveCart(Long userId, Cart cart) {
        try {
            String json = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(CART_KEY_PREFIX + userId, json);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to save cart to Redis", e);
        }
    }
}
