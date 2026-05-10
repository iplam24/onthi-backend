package com.onthi.v_edu.social.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.service.UserService;
import com.onthi.v_edu.wallet.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/social/gift")
@RequiredArgsConstructor
public class GiftController {

    private final WalletService walletService;
    private final UserService userService;
    private final com.onthi.v_edu.social.service.NotificationService notificationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> sendGift(
            @RequestBody GiftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User sender = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        User receiver = userService.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        try {
            walletService.transfer(sender, receiver, request.getAmount(), request.getMessage());
            
            // Notify receiver
            notificationService.createNotification(
                receiver,
                "Quà tặng mới!",
                sender.getUsername() + " đã tặng bạn " + request.getAmount() + "đ kèm lời nhắn: " + request.getMessage(),
                "GIFT",
                "/profile"
            );

            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã tặng quà thành công cho " + receiver.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    @Data
    public static class GiftRequest {
        private Integer receiverId;
        private BigDecimal amount;
        private String message;
    }
}
