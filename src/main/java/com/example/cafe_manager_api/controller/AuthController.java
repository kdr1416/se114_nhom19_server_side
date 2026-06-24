package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.LoginRequest;
import com.example.cafe_manager_api.dto.LoginResponse;
import com.example.cafe_manager_api.dto.UserProfileResponse;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "INVALID_CREDENTIALS", "message", ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "UNAUTHORIZED", "message", "Chưa xác thực"));
        }
        UserEntity user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng."));
        
        UserProfileResponse profile = new UserProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
        return ResponseEntity.ok(profile);
    }
}
