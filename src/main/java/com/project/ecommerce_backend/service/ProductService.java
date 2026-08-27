package com.project.ecommerce_backend.service;


import com.project.ecommerce_backend.dto.ProductRequest;
import com.project.ecommerce_backend.dto.ProductResponse;
import com.project.ecommerce_backend.entity.Product;
import com.project.ecommerce_backend.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final S3Service s3Service;

    public ProductService(ProductRepository productRepository, S3Service s3Service) {
        this.productRepository = productRepository;
        this.s3Service = s3Service;
    }

    @Cacheable(value = "allProducts")
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        System.out.println(">>> getAllProducts() is EXECUTING (cache miss or no cache)");
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "product", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {@CacheEvict(value = "allProducts", allEntries = true)})
    @Transactional
    public ProductResponse createProduct(ProductRequest request, MultipartFile image) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());

        if(image != null && !image.isEmpty()) {
            String  imageUrl = s3Service.uploadFile(image);
            product.setImageUrl(imageUrl);
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "allProducts", allEntries = true)
    })
    @Transactional
    public ProductResponse updatedProduct(Long id,
                                          ProductRequest request,
                                          MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        //del old img if new one id uploaded:
        if(image != null && !image.isEmpty() && product.getImageUrl() != null) {
            s3Service.deleteFile(product.getImageUrl());
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());

        if(image != null && !image.isEmpty()) {
            String imageUrl = s3Service.uploadFile(image);
            product.setImageUrl(imageUrl);
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }


    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "allProducts", allEntries = true)
    })
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if(product.getImageUrl() != null) {
            s3Service.deleteFile(product.getImageUrl());
        }
        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                    product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }


}
