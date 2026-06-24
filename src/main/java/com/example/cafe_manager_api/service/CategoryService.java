package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.CategoryRequest;
import com.example.cafe_manager_api.dto.CategoryResponse;
import com.example.cafe_manager_api.entity.CategoryEntity;
import com.example.cafe_manager_api.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    private CategoryResponse mapToResponse(CategoryEntity category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription(),
                category.getIsActive()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        CategoryEntity category = new CategoryEntity();
        category.setCategoryName(request.getCategoryName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setIsActive(true);

        CategoryEntity savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục với ID: " + id));

        category.setCategoryName(request.getCategoryName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        CategoryEntity updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    @Transactional
    public void softDeleteCategory(Integer id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục với ID: " + id));
        category.setIsActive(false);
        categoryRepository.save(category);
    }
}
