package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.ShiftTemplateRequest;
import com.example.cafe_manager_api.dto.ShiftTemplateResponse;
import com.example.cafe_manager_api.entity.ShiftTemplateEntity;
import com.example.cafe_manager_api.repository.ShiftTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftTemplateService {

    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;

    private ShiftTemplateResponse mapToResponse(ShiftTemplateEntity template) {
        return new ShiftTemplateResponse(
                template.getTemplateId(),
                template.getTemplateName(),
                template.getStartTime(),
                template.getEndTime(),
                template.getMinStaff(),
                template.getIsActive(),
                template.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ShiftTemplateResponse> getAllTemplates() {
        return shiftTemplateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShiftTemplateResponse getTemplateById(Integer id) {
        ShiftTemplateEntity template = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca mẫu với ID: " + id));
        return mapToResponse(template);
    }

    @Transactional
    public ShiftTemplateResponse createTemplate(ShiftTemplateRequest request) {
        ShiftTemplateEntity template = new ShiftTemplateEntity();
        template.setTemplateName(request.getTemplateName().trim());
        template.setStartTime(request.getStartTime().trim());
        template.setEndTime(request.getEndTime().trim());
        template.setMinStaff(request.getMinStaff());
        template.setIsActive(request.getIsActive());
        template.setCreatedAt(System.currentTimeMillis());

        ShiftTemplateEntity saved = shiftTemplateRepository.save(template);
        return mapToResponse(saved);
    }

    @Transactional
    public ShiftTemplateResponse updateTemplate(Integer id, ShiftTemplateRequest request) {
        ShiftTemplateEntity template = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca mẫu với ID: " + id));

        template.setTemplateName(request.getTemplateName().trim());
        template.setStartTime(request.getStartTime().trim());
        template.setEndTime(request.getEndTime().trim());
        template.setMinStaff(request.getMinStaff());
        template.setIsActive(request.getIsActive());

        ShiftTemplateEntity updated = shiftTemplateRepository.save(template);
        return mapToResponse(updated);
    }

    @Transactional
    public void deactivateTemplate(Integer id) {
        ShiftTemplateEntity template = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca mẫu với ID: " + id));
        template.setIsActive(false);
        shiftTemplateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Integer id) {
        if (!shiftTemplateRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy ca mẫu với ID: " + id);
        }
        shiftTemplateRepository.deleteById(id);
    }
}
