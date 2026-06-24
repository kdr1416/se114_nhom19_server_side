package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Set;

@Repository
public interface NewsReadRepository extends JpaRepository<NewsReadEntity, Integer> {
    Optional<NewsReadEntity> findByPostIdAndUserId(Integer postId, Integer userId);

    @Query("SELECT nr.postId FROM NewsReadEntity nr WHERE nr.userId = :userId")
    Set<Integer> findReadPostIdsByUserId(@Param("userId") Integer userId);
}
