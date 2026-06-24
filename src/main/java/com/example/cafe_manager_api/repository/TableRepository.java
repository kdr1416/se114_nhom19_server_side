package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, Integer> {
    List<TableEntity> findByArea(String area);
    List<TableEntity> findByStatus(String status);
    long countByArea(String areaName);

    @Modifying
    @Query("UPDATE TableEntity t SET t.area = :newArea WHERE t.area = :oldArea")
    void updateAreaName(@Param("newArea") String newArea, @Param("oldArea") String oldArea);
}
