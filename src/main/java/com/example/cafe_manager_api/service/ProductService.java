package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.ProductRequest;
import com.example.cafe_manager_api.dto.ProductResponse;
import com.example.cafe_manager_api.entity.ProductEntity;
import com.example.cafe_manager_api.repository.CategoryRepository;
import com.example.cafe_manager_api.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private ProductResponse mapToResponse(ProductEntity product) {
        return new ProductResponse(
                product.getProductId(),
                product.getCategoryId(),
                product.getProductName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getIsActive(),
                product.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(Integer categoryId, Boolean available) {
        List<ProductEntity> products;

        if (categoryId != null && available != null) {
            products = productRepository.findByCategoryIdAndIsActive(categoryId, available);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (available != null) {
            products = productRepository.findByIsActive(available);
        } else {
            products = productRepository.findAll();
        }

        return products.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new EntityNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId());
        }

        ProductEntity product = new ProductEntity();
        product.setCategoryId(request.getCategoryId());
        product.setProductName(request.getProductName().trim());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        product.setIsActive(true);
        product.setCreatedAt(System.currentTimeMillis());

        ProductEntity savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new EntityNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId());
        }

        product.setCategoryId(request.getCategoryId());
        product.setProductName(request.getProductName().trim());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        ProductEntity updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void softDeleteProduct(Integer id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }
}
