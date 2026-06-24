package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.NewsPostResponse;
import com.example.cafe_manager_api.dto.CreateNewsRequest;
import com.example.cafe_manager_api.dto.UpdateNewsRequest;
import com.example.cafe_manager_api.entity.NewsPostEntity;
import com.example.cafe_manager_api.entity.NewsReadEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.NewsPostRepository;
import com.example.cafe_manager_api.repository.NewsReadRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsPostRepository newsPostRepository;

    @Autowired
    private NewsReadRepository newsReadRepository;

    @Autowired
    private UserRepository userRepository;

    private NewsPostResponse mapToResponse(NewsPostEntity post, boolean isRead, String authorName) {
        return new NewsPostResponse(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getType(),
                post.getPriority(),
                post.getTargetType(),
                post.getTargetRole(),
                post.getTargetShiftId(),
                post.getCreatedByUserId(),
                authorName,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getIsPinned(),
                post.getIsDeleted(),
                isRead
        );
    }

    @Transactional(readOnly = true)
    public List<NewsPostResponse> getAllPostsForUser(String username) {
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        List<NewsPostEntity> posts = newsPostRepository.findAllActive();
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> tempRead = newsReadRepository.findReadPostIdsByUserId(currentUser.getUserId());
        final Set<Integer> readPostIds = tempRead != null ? tempRead : new HashSet<>();

        List<Integer> authorIds = posts.stream()
                .map(NewsPostEntity::getCreatedByUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> authorNames = userRepository.findAllByUserIdIn(authorIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getFullName));

        return posts.stream()
                .map(post -> mapToResponse(
                        post,
                        readPostIds.contains(post.getPostId()),
                        authorNames.getOrDefault(post.getCreatedByUserId(), "Unknown")
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NewsPostResponse getPostByIdForUser(Integer id, String username) {
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        NewsPostEntity post = newsPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông báo."));

        boolean isRead = newsReadRepository.findByPostIdAndUserId(id, currentUser.getUserId()).isPresent();
        String authorName = userRepository.findById(post.getCreatedByUserId())
                .map(UserEntity::getFullName)
                .orElse("Unknown");

        return mapToResponse(post, isRead, authorName);
    }

    @Transactional
    public NewsPostResponse createPost(CreateNewsRequest request, String username) {
        UserEntity author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        NewsPostEntity post = new NewsPostEntity();
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setType(request.getType());
        post.setPriority(request.getPriority());
        post.setTargetType(request.getTargetType());
        post.setTargetRole(request.getTargetRole());
        post.setTargetShiftId(request.getTargetShiftId());
        post.setIsPinned(request.isPinned());
        post.setCreatedByUserId(author.getUserId());
        post.setCreatedAt(System.currentTimeMillis());
        post.setIsDeleted(false);
        post.setUpdatedAt(null);

        NewsPostEntity saved = newsPostRepository.save(post);
        return mapToResponse(saved, false, author.getFullName());
    }

    @Transactional
    public NewsPostResponse updatePost(Integer id, UpdateNewsRequest request, String username) {
        UserEntity requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        NewsPostEntity post = newsPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông báo."));

        boolean isAdminOrManager = "ADMIN".equalsIgnoreCase(requester.getRole())
                || "MANAGER".equalsIgnoreCase(requester.getRole());
        boolean isAuthor = requester.getUserId().equals(post.getCreatedByUserId());
        if (!isAdminOrManager && !isAuthor) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật thông báo này.");
        }

        if (request.getTitle() != null) post.setTitle(request.getTitle().trim());
        if (request.getContent() != null) post.setContent(request.getContent().trim());
        if (request.getType() != null) post.setType(request.getType());
        if (request.getPriority() != null) post.setPriority(request.getPriority());
        if (request.getTargetType() != null) {
            post.setTargetType(request.getTargetType());
            if ("ROLE".equals(request.getTargetType())) {
                post.setTargetRole(request.getTargetRole());
                post.setTargetShiftId(null);
            } else if ("SHIFT".equals(request.getTargetType())) {
                post.setTargetShiftId(request.getTargetShiftId());
                post.setTargetRole(null);
            } else {
                post.setTargetRole(null);
                post.setTargetShiftId(null);
            }
        }
        if (request.getIsPinned() != null) post.setIsPinned(request.getIsPinned());

        post.setUpdatedAt(System.currentTimeMillis());
        NewsPostEntity saved = newsPostRepository.save(post);

        String authorName = userRepository.findById(post.getCreatedByUserId())
                .map(UserEntity::getFullName)
                .orElse("Unknown");
        boolean isRead = newsReadRepository.findByPostIdAndUserId(id, requester.getUserId()).isPresent();

        return mapToResponse(saved, isRead, authorName);
    }

    @Transactional
    public void deletePost(Integer id, String username) {
        UserEntity requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        NewsPostEntity post = newsPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông báo."));

        boolean isAdminOrManager = "ADMIN".equalsIgnoreCase(requester.getRole())
                || "MANAGER".equalsIgnoreCase(requester.getRole());
        boolean isAuthor = requester.getUserId().equals(post.getCreatedByUserId());
        if (!isAdminOrManager && !isAuthor) {
            throw new AccessDeniedException("Bạn không có quyền xóa thông báo này.");
        }

        post.setIsDeleted(true);
        newsPostRepository.save(post);
    }

    @Transactional
    public void markRead(Integer postId, Integer userId) {
        boolean exists = newsReadRepository.findByPostIdAndUserId(postId, userId).isPresent();
        if (!exists) {
            NewsReadEntity read = new NewsReadEntity();
            read.setPostId(postId);
            read.setUserId(userId);
            read.setReadAt(System.currentTimeMillis());
            newsReadRepository.save(read);
        }
    }

    @Transactional(readOnly = true)
    public Integer getUnreadCount(Integer userId) {
        Long count = newsPostRepository.countUnreadByUserId(userId);
        return count != null ? count.intValue() : 0;
    }
}