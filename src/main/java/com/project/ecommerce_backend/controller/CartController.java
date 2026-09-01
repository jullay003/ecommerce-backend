package com.project.ecommerce_backend.controller;


import com.project.ecommerce_backend.dto.Cart;
import com.project.ecommerce_backend.dto.CartItemRequest;
import com.project.ecommerce_backend.security.UserPrincipal;
import com.project.ecommerce_backend.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@RequestBody CartItemRequest request, Authentication authentication) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.addItem(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items")
    public ResponseEntity<Cart> updateItemQuantity(@RequestBody CartItemRequest request, Authentication authentication) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.updateItemQuantity(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(@PathVariable Long productId, Authentication authentication) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }



    private Long getUserId(Authentication authentication) {
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("Principal is not UserPrincipal");
    }



}
