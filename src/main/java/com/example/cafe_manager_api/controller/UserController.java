package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.CreateUserRequest;
import com.example.cafe_manager_api.dto.UpdateUserRequest;
import com.example.cafe_manager_api.dto.ChangePasswordRequest;
import com.example.cafe_manager_api.dto.UpdateUserStatusRequest;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserEntity>> getAllUsers(Principal principal) {
        List<UserEntity> users = userService.getUsersForRequester(principal.getName());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserEntity> createUser(@Valid @RequestBody CreateUserRequest request, Principal principal) {
        UserEntity user = userService.createUser(request, principal.getName());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserDetail(@PathVariable Integer id, Principal principal) {
        UserEntity user = userService.getUserDetail(id, principal.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request,
            Principal principal) {
        UserEntity user = userService.updateUser(id, request, principal.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable Integer id,
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        userService.changePassword(id, request, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Principal principal) {
        userService.updateUserStatus(id, request, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái tài khoản thành công."));
    }
}
