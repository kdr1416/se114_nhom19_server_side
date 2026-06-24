package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.AreaResponse;
import com.example.cafe_manager_api.dto.CreateAreaRequest;
import com.example.cafe_manager_api.dto.UpdateAreaRequest;
import com.example.cafe_manager_api.entity.AreaEntity;
import com.example.cafe_manager_api.entity.TableEntity;
import com.example.cafe_manager_api.repository.AreaRepository;
import com.example.cafe_manager_api.repository.TableRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private TableRepository tableRepository;

    @Transactional(readOnly = true)
    public List<AreaResponse> getAllAreas() {
        List<AreaEntity> areas = areaRepository.findAll();
        return areas.stream()
                .map(area -> mapToResponse(area, (int) tableRepository.countByArea(area.getAreaName())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AreaResponse getAreaById(Integer id) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khu vực không tồn tại."));
        int count = (int) tableRepository.countByArea(area.getAreaName());
        return mapToResponse(area, count);
    }

    @Transactional
    public AreaResponse createArea(CreateAreaRequest request) {
        AreaEntity area = new AreaEntity();
        area.setAreaName(request.getAreaName().trim());
        area.setPrefix(request.getPrefix().trim());
        area.setDescription(request.getDescription());
        area.setCreatedAt(System.currentTimeMillis());

        AreaEntity saved = areaRepository.save(area);
        return mapToResponse(saved, 0);
    }

    @Transactional
    public AreaResponse updateArea(Integer id, UpdateAreaRequest request) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khu vực không tồn tại."));

        String originalAreaName = area.getAreaName(); // save BEFORE changes

        if (request.getAreaName() != null) {
            area.setAreaName(request.getAreaName().trim());
        }
        if (request.getPrefix() != null) {
            area.setPrefix(request.getPrefix().trim());
        }
        if (request.getDescription() != null) {
            area.setDescription(request.getDescription());
        }

        AreaEntity saved = areaRepository.save(area);

        // Cascade: update table area names if areaName changed
        if (!originalAreaName.equals(saved.getAreaName())) {
            tableRepository.updateAreaName(saved.getAreaName(), originalAreaName);
        }

        int count = (int) tableRepository.countByArea(saved.getAreaName());
        return mapToResponse(saved, count);
    }

    @Transactional
    public void deleteArea(Integer id) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khu vực không tồn tại."));

        long tableCount = tableRepository.countByArea(area.getAreaName());
        if (tableCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa khu vực đang chứa " + tableCount + " bàn.");
        }

        areaRepository.delete(area);
    }

    private AreaResponse mapToResponse(AreaEntity area, int tableCount) {
        return new AreaResponse(
                area.getAreaId(),
                area.getAreaName(),
                area.getPrefix(),
                area.getDescription(),
                tableCount
        );
    }
}