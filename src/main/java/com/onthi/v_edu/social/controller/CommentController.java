package com.onthi.v_edu.social.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.social.entity.Comment;
import com.onthi.v_edu.social.service.CommentService;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Comment>> addComment(
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Comment comment = commentService.addComment(user, request.getPostId(), request.getParentId(), request.getContent());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã thêm bình luận", comment));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<Comment>>> getComments(@PathVariable Integer postId) {
        List<Comment> comments = commentService.getCommentsByPost(postId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy bình luận thành công", comments));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Comment>> updateComment(
            @PathVariable Integer id,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Comment comment = commentService.updateComment(user, id, request.getContent());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã cập nhật bình luận", comment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        commentService.deleteComment(user, id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã xóa bình luận"));
    }

    @Data
    public static class CommentRequest {
        private Integer postId;
        private Integer parentId;
        private String content;
    }
}
