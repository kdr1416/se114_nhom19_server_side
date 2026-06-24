package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {
    List<ProductEntity> findByCategoryId(Integer categoryId);
    List<ProductEntity> findByIsActive(Boolean isActive);
    List<ProductEntity> findByCategoryIdAndIsActive(Integer categoryId, Boolean isActive);
}
