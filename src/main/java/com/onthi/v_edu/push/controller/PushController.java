package com.onthi.v_edu.push.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.push.dto.PushSubscriptionRequest;
import com.onthi.v_edu.push.entity.PushSubscription;
import com.onthi.v_edu.push.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionRepository subscriptionRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<String>> subscribe(
            @RequestBody PushSubscriptionRequest request,
            java.security.Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(request.getEndpoint());

        PushSubscription subscription = existing.orElse(new PushSubscription());
        subscription.setUserId(request.getUserId());
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuth(request.getAuth());

        subscriptionRepository.save(subscription);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đăng ký nhận thông báo thành công", null));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<String>> unsubscribe(@RequestBody String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
        return ResponseEntity
                .ok(new ApiResponse<>(HttpStatus.OK.value(), "Hủy đăng ký nhận thông báo thành công", null));
    }
}
