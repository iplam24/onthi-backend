package com.onthi.v_edu.user.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.user.service.UserService;
import com.onthi.v_edu.user.service.UserService.BalanceUpdateRequest;
import com.onthi.v_edu.user.service.UserService.UserInformationRequest;
import com.onthi.v_edu.user.service.UserService.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query) {
        Pageable pageable = PageRequest.of(page, size);
        ApiResponse<Page<UserProfileResponse>> response = userService.getAllUsers(pageable, query);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable Integer id,
            @RequestParam boolean enabled) {
        ApiResponse<UserProfileResponse> response = userService.updateUserStatus(id, enabled);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserBalance(
            @PathVariable Integer id,
            @RequestBody BalanceUpdateRequest request) {
        ApiResponse<UserProfileResponse> response = userService.updateUserBalance(id, request.amount(), request.type());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserByAdmin(
            @PathVariable Integer id,
            @RequestBody UserInformationRequest request) {
        ApiResponse<UserProfileResponse> response = userService.updateUserInformationByAdmin(id, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
