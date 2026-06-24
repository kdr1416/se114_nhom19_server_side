package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.NewsPostResponse;
import com.example.cafe_manager_api.dto.CreateNewsRequest;
import com.example.cafe_manager_api.dto.UpdateNewsRequest;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NewsPostResponse>> getAllPosts(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        List<NewsPostResponse> posts = newsService.getAllPostsForUser(username);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NewsPostResponse> getPostById(@PathVariable Integer id, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        NewsPostResponse post = newsService.getPostByIdForUser(id, username);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<NewsPostResponse> createPost(@Valid @RequestBody CreateNewsRequest request, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        NewsPostResponse response = newsService.createPost(request, username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<NewsPostResponse> updatePost(@PathVariable Integer id, @Valid @RequestBody UpdateNewsRequest request, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        NewsPostResponse response = newsService.updatePost(id, request, username);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deletePost(@PathVariable Integer id, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        newsService.deletePost(id, username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable Integer id, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));
        newsService.markRead(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Integer> getUnreadCount(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        String username = principal.getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));
        Integer count = newsService.getUnreadCount(user.getUserId());
        return ResponseEntity.ok(count);
    }
}