package com.onthi.v_edu.social.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.social.service.FollowService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final UserService userService;

    @PostMapping("/follow/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> follow(@PathVariable Integer userId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        followService.follow(currentUser.getId(), userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã theo dõi người dùng này"));
    }

    @PostMapping("/unfollow/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable Integer userId, @AuthenticationPrincipal UserDetails userDetails) {
        User follower = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        followService.unfollow(follower.getId(), userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã bỏ theo dõi"));
    }

    @PostMapping("/remove-follower/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeFollower(@PathVariable Integer userId, @AuthenticationPrincipal UserDetails userDetails) {
        User following = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        followService.removeFollower(following.getId(), userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã xóa người theo dõi"));
    }

    @PostMapping("/remove-friend/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeFriend(@PathVariable Integer userId, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        followService.removeFriend(user.getId(), userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã hủy kết bạn"));
    }

    @GetMapping("/is-following/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(@PathVariable Integer userId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        
        boolean following = followService.isFollowing(currentUser.getId(), userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Trạng thái theo dõi", following));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getFollowers(@PathVariable Integer userId, Pageable pageable) {
        Page<User> followers = followService.getFollowers(userId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Danh sách người theo dõi", PageResponse.from(followers)));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getFollowing(@PathVariable Integer userId, Pageable pageable) {
        Page<User> following = followService.getFollowing(userId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Danh sách đang theo dõi", PageResponse.from(following)));
    }

    @GetMapping("/friends/{userId}")
    public ResponseEntity<ApiResponse<Page<User>>> getFriends(@PathVariable Integer userId, Pageable pageable) {
        Page<User> friends = followService.getFriends(userId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Danh sách bạn bè", friends));
    }

    @GetMapping("/follow-stats/{userId}")
    public ResponseEntity<ApiResponse<FollowStats>> getFollowStats(@PathVariable Integer userId) {
        long followers = followService.getFollowerCount(userId);
        long following = followService.getFollowingCount(userId);
        long friends = followService.getFriendCount(userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Thống kê theo dõi", new FollowStats(followers, following, friends)));
    }

    private record FollowStats(long followers, long following, long friends) {}
}
