package com.example.cafe_manager_api.security;

import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    // TODO: implement preHandle to check authentication if needed
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Placeholder: allow all requests
        return true;
    }
}
