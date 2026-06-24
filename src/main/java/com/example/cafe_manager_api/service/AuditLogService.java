package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.CreateAuditLogRequest;
import com.example.cafe_manager_api.entity.AuditLogEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.AuditLogRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AuditLogEntity> getAllLogs() {
        return auditLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> getMyLogs(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng: " + username));
        return auditLogRepository.findByUserId(user.getUserId());
    }

    @Transactional(readOnly = true)
    public AuditLogEntity getLogById(Integer id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhật ký hệ thống: " + id));
    }

    @Transactional
    public AuditLogEntity createLog(String username, CreateAuditLogRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng: " + username));
        
        AuditLogEntity log = new AuditLogEntity();
        log.setUserId(user.getUserId());
        log.setAction(request.getAction());
        log.setTargetType(request.getTargetType());
        log.setTargetId(request.getTargetId());
        log.setDescription(request.getDescription());
        log.setCreatedAt(System.currentTimeMillis());
        
        // TODO: Call createLog from other services (OrderService, PaymentService, ShiftService) after important actions.
        
        return auditLogRepository.save(log);
    }
}
