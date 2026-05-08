package com.onthi.v_edu.wallet.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.wallet.dto.PaymentRequest;
import com.onthi.v_edu.wallet.entity.Transaction;
import com.onthi.v_edu.wallet.service.PayOSService;
import com.onthi.v_edu.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final WalletService walletService;
    private final PayOSService payOSService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> createPayment(@RequestBody PaymentRequest request) {
        try {
            User user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null));
            }
            
            String checkoutUrl = walletService.initiateDeposit(user, request.getAmount());
            logger.info("[API PAYMENT] Trả về link thanh toán thành công cho user: {}", user.getUsername());
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Payment link created", checkoutUrl));
        } catch (Exception e) {
            logger.error("Error creating payment link: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error: " + e.getMessage(), null));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Webhook webhookBody) {
        try {
            logger.info("[API WEBHOOK] Nhận yêu cầu webhook từ PayOS: {}", webhookBody);
            WebhookData verifiedData = payOSService.verifyWebhookData(webhookBody);
            walletService.processPaymentWebhook(verifiedData);
            logger.info("[API WEBHOOK] Xử lý webhook thành công cho OrderCode: {}", verifiedData.getOrderCode());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Error processing webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/status/{orderCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getPaymentStatus(@PathVariable Long orderCode) {
        try {
            PaymentLink data = payOSService.getPaymentLinkInformation(orderCode);
            // Đồng bộ trạng thái vào DB ngay khi kiểm tra (Hữu ích khi chạy localhost không có webhook)
            walletService.syncPaymentStatus(orderCode, data);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Status retrieved", data));
        } catch (Exception e) {
            logger.error("Error getting payment status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error: " + e.getMessage(), null));
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getMyTransactions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<Transaction> transactions = walletService.getTransactionsByUser(user, pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Transactions retrieved", transactions));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
