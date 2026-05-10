package com.onthi.v_edu.user.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.user.service.UserService;
import com.onthi.v_edu.user.service.UserService.UserInformationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMyProfile() {
        ApiResponse<?> response = userService.getMyProfile();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/me/information")
    public ResponseEntity<ApiResponse<?>> updateMyInformation(@RequestBody UserInformationRequest request) {
        ApiResponse<?> response = userService.updateMyInformation(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/me/streak")
    public ResponseEntity<ApiResponse<?>> getMyStreak() {
        ApiResponse<?> response = userService.getMyStreak();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/me/streak/check-in")
    public ResponseEntity<ApiResponse<?>> checkInMyStreak() {
        ApiResponse<?> response = userService.checkInMyStreak();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getUserProfile(@PathVariable Integer id) {
        ApiResponse<?> response = userService.getUserProfile(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchUsers(@RequestParam String query) {
        ApiResponse<?> response = userService.searchUsers(query);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
