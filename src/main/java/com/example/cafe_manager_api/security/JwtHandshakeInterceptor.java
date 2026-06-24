package com.example.cafe_manager_api.security;

import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            if (username != null) {
                UserEntity user = userRepository.findByUsername(username).orElse(null);
                if (user != null && Boolean.TRUE.equals(user.getIsActive())) {
                    attributes.put("userId", user.getUserId());
                    attributes.put("username", username);
                    attributes.put("role", user.getRole());

                    // Set custom Principal so STOMP can recognize authenticated user
                    Principal principal = new Principal() {
                        @Override
                        public String getName() {
                            return username;
                        }
                    };
                    attributes.put("user", principal);
                    return true;
                }
            }
        }
        return false; // Reject handshake if missing or invalid token
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Do nothing
    }

    private String extractToken(ServerHttpRequest request) {
        // 1. Try from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Try from query parameters (common in web ws client)
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String token = servletRequest.getParameter("token");
            if (token != null) {
                if (token.startsWith("Bearer ")) {
                    return token.substring(7);
                }
                return token;
            }
        }
        return null;
    }
}
