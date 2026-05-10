package com.onthi.v_edu.social.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.social.entity.Notification;
import com.onthi.v_edu.social.service.NotificationService;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.service.UserService;
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
@RequestMapping("/api/social/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<Notification> notifications = notificationService.getMyNotifications(user, PageRequest.of(page, size));
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy thông báo thành công", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy số lượng thông báo chưa đọc", notificationService.getUnreadCount(user)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã đánh dấu là đã đọc"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã đánh dấu tất cả là đã đọc"));
    }
}
