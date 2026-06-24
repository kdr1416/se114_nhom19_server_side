package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.TableRequest;
import com.example.cafe_manager_api.dto.TableResponse;
import com.example.cafe_manager_api.entity.TableEntity;
import com.example.cafe_manager_api.repository.TableRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableService {

    @Autowired
    private TableRepository tableRepository;

    private TableResponse mapToResponse(TableEntity table) {
        return new TableResponse(
                table.getTableId(),
                table.getTableName(),
                table.getStatus(),
                table.getCapacity(),
                table.getArea(),
                table.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables(String status) {
        List<TableEntity> tables;
        if (status != null && !status.trim().isEmpty()) {
            tables = tableRepository.findByStatus(status.trim().toUpperCase());
        } else {
            tables = tableRepository.findAll();
        }
        return tables.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TableResponse getTableById(Integer id) {
        TableEntity table = tableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + id));
        return mapToResponse(table);
    }

    @Transactional
    public TableResponse createTable(TableRequest request) {
        TableEntity table = new TableEntity();
        table.setTableName(request.getTableName().trim());
        table.setStatus(request.getStatus().trim().toUpperCase());
        table.setCapacity(request.getCapacity());
        table.setArea(request.getArea().trim());
        table.setCreatedAt(System.currentTimeMillis());

        TableEntity savedTable = tableRepository.save(table);
        return mapToResponse(savedTable);
    }

    @Transactional
    public TableResponse updateTable(Integer id, TableRequest request) {
        TableEntity table = tableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + id));

        table.setTableName(request.getTableName().trim());
        table.setStatus(request.getStatus().trim().toUpperCase());
        table.setCapacity(request.getCapacity());
        table.setArea(request.getArea().trim());

        TableEntity updatedTable = tableRepository.save(table);
        return mapToResponse(updatedTable);
    }

    @Transactional
    public void deleteTable(Integer id) {
        if (!tableRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy bàn với ID: " + id);
        }
        tableRepository.deleteById(id);
    }
}
