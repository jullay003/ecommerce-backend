package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.ProductRequest;
import com.project.ecommerce_backend.dto.ProductResponse;
import com.project.ecommerce_backend.entity.Product;
import com.project.ecommerce_backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        // Initialize mocks manually – works with any JUnit/Mockito version
        MockitoAnnotations.openMocks(this);

        product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setDescription("High performance laptop");
        product.setPrice(new BigDecimal("1299.99"));
        product.setStockQuantity(10);
        product.setCategory("Electronics");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllProducts_ShouldReturnListOfProductResponse() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(product));
        List<ProductResponse> responses = productService.getAllProducts();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_WhenProductExists_ShouldReturnProductResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        ProductResponse response = productService.getProductById(1L);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_WhenProductNotFound_ShouldThrowException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
        verify(productRepository, times(1)).findById(99L);
    }

    @Test
    void createProduct_ShouldSaveProductAndReturnResponse() {
        ProductRequest request = new ProductRequest();
        request.setName("Phone");
        request.setDescription("Smartphone");
        request.setPrice(new BigDecimal("699.99"));
        request.setStockQuantity(5);
        request.setCategory("Electronics");

        Product savedProduct = new Product();
        savedProduct.setId(2L);
        savedProduct.setName("Phone");
        savedProduct.setDescription("Smartphone");
        savedProduct.setPrice(new BigDecimal("699.99"));
        savedProduct.setStockQuantity(5);
        savedProduct.setCategory("Electronics");

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        ProductResponse response = productService.createProduct(request, null);
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("Phone");
        verify(productRepository, times(1)).save(any(Product.class));
        verify(s3Service, never()).uploadFile(any(MultipartFile.class));
    }

    @Test
    void updateProduct_WhenProductExists_ShouldUpdateAndReturnResponse() {
        ProductRequest request = new ProductRequest();
        request.setName("Gaming Laptop");
        request.setDescription("Updated");
        request.setPrice(new BigDecimal("1499.99"));
        request.setStockQuantity(8);
        request.setCategory("Electronics");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        ProductResponse response = productService.updatedProduct(1L, request, null);
        assertThat(response.getName()).isEqualTo("Gaming Laptop");
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void deleteProduct_WhenProductExists_ShouldDelete() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);
        productService.deleteProduct(1L);
        verify(productRepository, times(1)).delete(product);
        verify(s3Service, never()).deleteFile(anyString());
    }
}