package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.Cart;
import com.project.ecommerce_backend.dto.CartItem;
import com.project.ecommerce_backend.entity.Product;
import com.project.ecommerce_backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Optional;

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

    @Test
    void getCart_WhenCartExists_ShouldReturnCartFromRedis() throws Exception {
        //Arrange
        Long userId = 1L;
        Cart existingCart = new Cart(userId, new java.util.ArrayList<>());
        existingCart.getItems().add(new CartItem(10L, 2));
        String json = objectMapper.writeValueAsString(existingCart);
        when(valueOperations.get("cart:1")).thenReturn(json);

        //Act
        Cart cart = cartService.getCart(userId);

        //Assert
        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_WhenProductExistsAndNotInCart_ShouldAddNewItem() {
        //Arrange:
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 3;

        Cart emptyCart = new Cart(userId, new ArrayList<>());
        when(valueOperations.get("cart:1")).thenReturn(null);
        Product product = new Product();
        product.setId(productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(valueOperations).set(anyString(), anyString());

        //Act
        Cart result = cartService.addItem(userId, productId, quantity);

        //Assert
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(quantity);
        verify(valueOperations, times(1)).set(eq("cart:1"), anyString());
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void addIttem_WhenProductDoesNotExist_ShouldThrowException() {
        //Arrange
        Long userId = 1L;
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //Act & assert
        assertThatThrownBy(() -> cartService.addItem(userId, productId, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
        verify(valueOperations, never()).set(anyString(), anyString());

    }

    @Test
    void updateItemQuantity_WhenItemExists_ShouldUpdateQuantity() {
        //Arrange
        Long userId = 1L;
        Long productId = 10L;
        Cart existingCart = new Cart(userId, new ArrayList<>());
        existingCart.getItems().add(new CartItem(productId, 2));
        String json = "{\"userId\":1,\"items\":[{\"productId\":10, \"quantity\":2}]}";
        when(valueOperations.get("cart:1")).thenReturn(json);
        doNothing().when(valueOperations).set(anyString(), anyString());

        //Act
        Cart result = cartService.updateItemQuantity(userId, productId, 5);

        //Assert
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(valueOperations, times(1)).set(eq("cart:1"), anyString());
    }

    @Test
    void removeItem_WhenItemExists_ShouldRemoveIt() {
        //Arrange
        Long userId = 1L;
        Long productId = 10L;
        Cart existingCart = new Cart(userId, new ArrayList<>());
        existingCart.getItems().add(new CartItem(productId, 2));
        String json = "{\"userId\":1,\"items\":[{\"productId\":10, \"quantity\":2}]}";
        when(valueOperations.get("cart:1")).thenReturn(json);
        doNothing().when(valueOperations).set(anyString(), anyString());

        //Act
        Cart result = cartService.removeItem(userId, productId);

        //Assert
        assertThat(result.getItems()).isEmpty();
        verify(valueOperations, times(1)).set(eq("cart:1"), anyString());

    }

    @Test
    void clearCart_ShouldDeleteKey() {
        //Arrange
        Long userId = 1L;
        when(redisTemplate.delete(anyString())).thenReturn(true);

        //Act
        cartService.clearCart(userId);

        //Assert
        verify(redisTemplate, times(1)).delete("cart:1");
    }


}
