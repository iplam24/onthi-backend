package com.onthi.v_edu.social.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.social.entity.UserActivity;
import com.onthi.v_edu.social.service.UserActivityService;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social/feed")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityService userActivityService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<UserActivity>>> getFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<UserActivity> feed = userActivityService.getFeed(currentUser.getId(), pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy bảng tin thành công", PageResponse.from(feed)));
    }
}
