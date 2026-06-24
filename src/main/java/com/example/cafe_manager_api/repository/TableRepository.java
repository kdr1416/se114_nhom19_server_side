package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, Integer> {
    List<TableEntity> findByArea(String area);
    List<TableEntity> findByStatus(String status);
}
