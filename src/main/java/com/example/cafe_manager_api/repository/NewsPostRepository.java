package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsPostRepository extends JpaRepository<NewsPostEntity, Integer> {
    List<NewsPostEntity> findByIsDeletedFalseOrderByCreatedAtDesc();
}
