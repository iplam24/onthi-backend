package com.onthi.v_edu.wallet.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.wallet.entity.Plan;
import com.onthi.v_edu.wallet.entity.UserPlan;
import com.onthi.v_edu.wallet.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Plan>>> getAllPlans() {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Danh sách gói cước", planService.getAllPlans()));
    }

    @GetMapping("/my-plan")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserPlan>> getMyPlan() {
        User user = getCurrentUser();
        return planService.getActiveUserPlan(user.getId())
                .map(plan -> ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Gói cước hiện tại", plan)))
                .orElse(ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Bạn chưa đăng ký gói cước nào", null)));
    }

    @PostMapping("/buy/{planId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserPlan>> buyPlan(@PathVariable Integer planId) {
        try {
            User user = getCurrentUser();
            UserPlan userPlan = planService.purchasePlan(user, planId);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Mua gói cước thành công!", userPlan));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
