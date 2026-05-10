package com.onthi.v_edu.social.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.social.entity.Post;
import com.onthi.v_edu.social.service.PostService;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Post>> createPost(
            @RequestBody PostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Post post = postService.createPost(user, request.getTitle(), request.getContent(), request.getImages());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã đăng bài thành công", post));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Post>>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<Post> posts = postService.getFeed(user, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy bảng tin thành công", posts));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<Post>>> getUserPosts(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Integer requesterId = null;
        if (userDetails != null) {
            requesterId = userService.findByUsername(userDetails.getUsername())
                    .map(User::getId).orElse(null);
        }
        
        Page<Post> posts = postService.getUserPosts(userId, requesterId, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy bài viết của người dùng thành công", posts));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        postService.deletePost(user, id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã xóa bài viết"));
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        postService.likePost(user, id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã thích bài viết"));
    }

    @PostMapping("/{id}/unlike")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        postService.unlikePost(user, id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã bỏ thích bài viết"));
    }

    @GetMapping("/{id}/like-info")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getLikeInfo(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Integer userId = null;
        if (userDetails != null) {
            userId = userService.findByUsername(userDetails.getUsername())
                    .map(User::getId).orElse(null);
        }

        boolean liked = postService.isLikedByUser(id, userId);
        long count = postService.getLikeCount(id);

        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("liked", liked);
        info.put("count", count);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy thông tin lượt thích thành công", info));
    }

    @Data
    public static class PostRequest {
        private String title;
        private String content;
        private java.util.List<String> images;
    }
}
