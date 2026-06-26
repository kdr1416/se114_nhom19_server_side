package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.ShiftTemplateRequest;
import com.example.cafe_manager_api.dto.ShiftTemplateResponse;
import com.example.cafe_manager_api.entity.ShiftTemplateEntity;
import com.example.cafe_manager_api.repository.ShiftTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.repository.ShiftRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftTemplateService {

    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }

    private boolean timeSlotsOverlap(String start1Str, String end1Str, String start2Str, String end2Str) {
        int start1 = timeToMinutes(start1Str);
        int end1 = timeToMinutes(end1Str);
        int start2 = timeToMinutes(start2Str);
        int end2 = timeToMinutes(end2Str);
        
        boolean overnight1 = end1 <= start1;
        boolean overnight2 = end2 <= start2;
        
        if (!overnight1 && !overnight2) {
            return start1 < end2 && start2 < end1;
        }
        
        List<int[]> segments1 = new ArrayList<>();
        if (overnight1) {
            segments1.add(new int[]{start1, 1440});
            segments1.add(new int[]{0, end1});
        } else {
            segments1.add(new int[]{start1, end1});
        }
        
        List<int[]> segments2 = new ArrayList<>();
        if (overnight2) {
            segments2.add(new int[]{start2, 1440});
            segments2.add(new int[]{0, end2});
        } else {
            segments2.add(new int[]{start2, end2});
        }
        
        for (int[] s1 : segments1) {
            for (int[] s2 : segments2) {
                if (s1[0] < s2[1] && s2[0] < s1[1]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dateRangesOverlap(Long from1, Long to1, Long from2, Long to2) {
        long f1 = (from1 != null) ? from1 : Long.MIN_VALUE;
        long t1 = (to1 != null) ? to1 : Long.MAX_VALUE;
        long f2 = (from2 != null) ? from2 : Long.MIN_VALUE;
        long t2 = (to2 != null) ? to2 : Long.MAX_VALUE;
        return f1 <= t2 && f2 <= t1;
    }

    private void validateShiftTemplateBusinessRules(ShiftTemplateEntity template, Long oldEffectiveToDate, String oldStartTime, String oldEndTime) {
        // Rule 1: check overlap with other active templates
        List<ShiftTemplateEntity> otherActiveTemplates = shiftTemplateRepository.findAll().stream()
                .filter(t -> !t.getTemplateId().equals(template.getTemplateId()))
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());
                
        if (Boolean.TRUE.equals(template.getIsActive())) {
            for (ShiftTemplateEntity other : otherActiveTemplates) {
                if (timeSlotsOverlap(template.getStartTime(), template.getEndTime(), other.getStartTime(), other.getEndTime())) {
                    if (dateRangesOverlap(template.getEffectiveFromDate(), template.getEffectiveToDate(),
                                         other.getEffectiveFromDate(), other.getEffectiveToDate())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Khung giờ của ca mẫu này trùng với ca mẫu '%s' (%s - %s) đang hoạt động trong cùng khoảng thời gian.",
                                        other.getTemplateName(), other.getStartTime(), other.getEndTime()));
                    }
                }
            }
        }
        
        // Rule 2: check published/open shifts if time slot or effectiveToDate changed
        boolean timeSlotChanged = oldStartTime != null && (!oldStartTime.equals(template.getStartTime()) || !oldEndTime.equals(template.getEndTime()));
        boolean effectiveToDateChanged = (oldEffectiveToDate != null && !oldEffectiveToDate.equals(template.getEffectiveToDate())) 
                                         || (oldEffectiveToDate == null && template.getEffectiveToDate() != null);
                                         
        if (timeSlotChanged || effectiveToDateChanged) {
            List<ShiftEntity> shifts = shiftRepository.findByTemplateId(template.getTemplateId());
            for (ShiftEntity shift : shifts) {
                boolean isConflict = false;
                if (timeSlotChanged) {
                    if ("PUBLISHED".equalsIgnoreCase(shift.getStatus()) || "OPEN".equalsIgnoreCase(shift.getStatus())) {
                        isConflict = true;
                    }
                } else if (effectiveToDateChanged && template.getEffectiveToDate() != null) {
                    if (shift.getShiftDate() > template.getEffectiveToDate() && 
                        ("PUBLISHED".equalsIgnoreCase(shift.getStatus()) || "OPEN".equalsIgnoreCase(shift.getStatus()))) {
                        isConflict = true;
                    }
                }
                
                if (isConflict) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không thể thay đổi khung giờ hoặc kết thúc ca mẫu vì có ca làm việc liên quan đang ở trạng thái phát hành hoặc đang mở.");
                }
            }
        }
    }

    private ShiftTemplateResponse mapToResponse(ShiftTemplateEntity template) {
        return new ShiftTemplateResponse(
                template.getTemplateId(),
                template.getTemplateName(),
                template.getStartTime(),
                template.getEndTime(),
                template.getMinStaff(),
                template.getIsActive(),
                template.getEffectiveFromDate(),
                template.getEffectiveToDate(),
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
        template.setTemplateId(-1); // dummy ID for validation
        template.setTemplateName(request.getTemplateName().trim());
        template.setStartTime(request.getStartTime().trim());
        template.setEndTime(request.getEndTime().trim());
        template.setMinStaff(request.getMinStaff());
        template.setIsActive(request.getIsActive());
        template.setEffectiveFromDate(request.getEffectiveFromDate());
        template.setEffectiveToDate(request.getEffectiveToDate());
        template.setCreatedAt(System.currentTimeMillis());

        // Validate business rules
        validateShiftTemplateBusinessRules(template, null, null, null);

        template.setTemplateId(null); // Reset dummy ID
        ShiftTemplateEntity saved = shiftTemplateRepository.save(template);
        return mapToResponse(saved);
    }

    @Transactional
    public ShiftTemplateResponse updateTemplate(Integer id, ShiftTemplateRequest request) {
        ShiftTemplateEntity template = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca mẫu với ID: " + id));

        String oldStartTime = template.getStartTime();
        String oldEndTime = template.getEndTime();
        Long oldEffectiveToDate = template.getEffectiveToDate();

        template.setTemplateName(request.getTemplateName().trim());
        template.setStartTime(request.getStartTime().trim());
        template.setEndTime(request.getEndTime().trim());
        template.setMinStaff(request.getMinStaff());
        template.setIsActive(request.getIsActive());
        template.setEffectiveFromDate(request.getEffectiveFromDate());
        template.setEffectiveToDate(request.getEffectiveToDate());

        // Validate business rules
        validateShiftTemplateBusinessRules(template, oldEffectiveToDate, oldStartTime, oldEndTime);

        ShiftTemplateEntity updated = shiftTemplateRepository.save(template);
        return mapToResponse(updated);
    }

    @Transactional
    public void deactivateTemplate(Integer id) {
        ShiftTemplateEntity template = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca mẫu với ID: " + id));

        // Check if there are active shifts
        List<ShiftEntity> shifts = shiftRepository.findByTemplateId(id);
        for (ShiftEntity shift : shifts) {
            if ("PUBLISHED".equalsIgnoreCase(shift.getStatus()) || "OPEN".equalsIgnoreCase(shift.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể tắt hoạt động ca mẫu vì có ca làm việc liên quan đang ở trạng thái phát hành hoặc đang mở.");
            }
        }

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
