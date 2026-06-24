package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsPostRepository extends JpaRepository<NewsPostEntity, Integer> {
    List<NewsPostEntity> findByIsDeletedFalseOrderByCreatedAtDesc();

    @Query("SELECT p FROM NewsPostEntity p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    List<NewsPostEntity> findAllActive();

    @Query("SELECT COUNT(p) FROM NewsPostEntity p " +
           "LEFT JOIN NewsReadEntity nr ON p.postId = nr.postId AND nr.userId = :userId " +
           "WHERE p.isDeleted = false AND nr.readId IS NULL")
    long countUnreadByUserId(@Param("userId") Integer userId);
}
