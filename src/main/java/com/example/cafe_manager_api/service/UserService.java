package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.CreateUserRequest;
import com.example.cafe_manager_api.dto.UpdateUserRequest;
import com.example.cafe_manager_api.dto.ChangePasswordRequest;
import com.example.cafe_manager_api.dto.UpdateUserStatusRequest;
import com.example.cafe_manager_api.entity.AuditLogEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.AuditLogRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Lazy
    @Autowired
    private ChatService chatService;

    private UserEntity getRequester(String requesterUsername) {
        return userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new AccessDeniedException("Người yêu cầu không tồn tại trong hệ thống."));
    }

    private void logAudit(Integer userId, String action, String targetType, String targetId, String description) {
        AuditLogEntity log = new AuditLogEntity();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setCreatedAt(System.currentTimeMillis());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getUsersForRequester(String requesterUsername) {
        UserEntity requester = getRequester(requesterUsername);
        String role = requester.getRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return userRepository.findAll();
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            return userRepository.findByRole("STAFF");
        } else {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách người dùng.");
        }
    }

    @Transactional
    public UserEntity createUser(CreateUserRequest request, String requesterUsername) {
        UserEntity requester = getRequester(requesterUsername);
        if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
            throw new AccessDeniedException("Chỉ Admin mới có quyền tạo người dùng.");
        }

        userRepository.findByUsername(request.getUsername().trim())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
                });

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setRole(request.getRole().toUpperCase());
        user.setIsActive(true);

        long now = System.currentTimeMillis();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserEntity savedUser = userRepository.save(user);

        logAudit(requester.getUserId(), "CREATE_USER", "USER", String.valueOf(savedUser.getUserId()), 
                "Tạo người dùng mới: " + savedUser.getUsername());

        chatService.addUserToSystemRooms(savedUser.getUserId(), savedUser.getRole());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public UserEntity getUserDetail(Integer id, String requesterUsername) {
        UserEntity requester = getRequester(requesterUsername);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));

        if ("ADMIN".equalsIgnoreCase(requester.getRole())) {
            return user;
        } else if ("MANAGER".equalsIgnoreCase(requester.getRole())) {
            if ("STAFF".equalsIgnoreCase(user.getRole())) {
                return user;
            } else {
                throw new AccessDeniedException("Quản lý chỉ có quyền xem thông tin của nhân viên.");
            }
        } else {
            // STAFF can view their own profile
            if (requester.getUserId().equals(user.getUserId())) {
                return user;
            }
            throw new AccessDeniedException("Bạn không có quyền xem thông tin người dùng này.");
        }
    }

    @Transactional
    public UserEntity updateUser(Integer id, UpdateUserRequest request, String requesterUsername) {
        UserEntity requester = getRequester(requesterUsername);
        if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
            throw new AccessDeniedException("Chỉ Admin mới có quyền cập nhật thông tin người dùng.");
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setRole(request.getRole().toUpperCase());
        user.setUpdatedAt(System.currentTimeMillis());

        UserEntity updatedUser = userRepository.save(user);

        logAudit(requester.getUserId(), "UPDATE_USER", "USER", String.valueOf(updatedUser.getUserId()), 
                "Cập nhật thông tin người dùng: " + updatedUser.getUsername());

        return updatedUser;
    }

    @Transactional
    public void changePassword(Integer id, ChangePasswordRequest request, String requesterUsername) {
        UserEntity requester = getRequester(requesterUsername);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(requester.getRole());
        boolean isSelf = requester.getUserId().equals(user.getUserId());

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("Bạn không có quyền đổi mật khẩu của người dùng này.");
        }

        if (isSelf && !isAdmin) {
            // Must verify old password
            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                throw new BadCredentialsException("Vui lòng cung cấp mật khẩu cũ.");
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new BadCredentialsException("Mật khẩu cũ không chính xác.");
            }
        }

        // Encode and set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);

        logAudit(requester.getUserId(), "RESET_PASSWORD", "USER", String.valueOf(user.getUserId()), 
                "Đổi mật khẩu cho người dùng: " + user.getUsername());
    }

    @Transactional
    public void updateUserStatus(Integer id, UpdateUserStatusRequest request, String requesterUsername) {
        UserEntity requester = getRequester(requesterUsername);
        if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
            throw new AccessDeniedException("Chỉ Admin mới có quyền bật/tắt trạng thái hoạt động người dùng.");
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));

        user.setIsActive(request.getIsActive());
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);

        String action = request.getIsActive() ? "UNLOCK_USER" : "LOCK_USER";
        String desc = request.getIsActive() ? "Mở khóa tài khoản: " : "Khóa tài khoản: ";
        logAudit(requester.getUserId(), action, "USER", String.valueOf(user.getUserId()), 
                desc + user.getUsername());
    }
}
