package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);
    java.util.List<UserEntity> findByRole(String role);

    @Query("SELECT u FROM UserEntity u WHERE u.userId IN :ids")
    List<UserEntity> findAllByUserIdIn(@Param("ids") List<Integer> ids);
}
