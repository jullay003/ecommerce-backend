package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.Cart;
import com.project.ecommerce_backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getCart_WhenNoCartExists_ShouldReturnEmptyCart() {
        //Arrange
        Long userId = 1L;
        when(valueOperations.get("cart:1")).thenReturn(null);

        //Act
        Cart cart = cartService.getCart(userId);

        //Assert
        assertThat(cart).isNotNull();
        assertThat(cart.getUserId()).isEqualTo(userId);
        assertThat(cart.getItems()).isEmpty();
        verify(valueOperations, times(1)).get("cart:1");
    }

}
