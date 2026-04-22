package com.onthi.v_edu.attempt.controller;

import com.onthi.v_edu.attempt.dto.AttemptStartRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitRequest;
import com.onthi.v_edu.attempt.service.AttemptService;
import com.onthi.v_edu.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attempts")
@PreAuthorize("isAuthenticated()")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<?>> startAttempt(@Valid @RequestBody AttemptStartRequest request) {
        ApiResponse<?> response = attemptService.startAttempt(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ApiResponse<?>> submitAttempt(@PathVariable Integer attemptId,
                                                        @Valid @RequestBody AttemptSubmitRequest request) {
        ApiResponse<?> response = attemptService.submitAttempt(attemptId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<ApiResponse<?>> getMyAttemptById(@PathVariable Integer attemptId) {
        ApiResponse<?> response = attemptService.getMyAttemptById(attemptId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMyAttempts() {
        ApiResponse<?> response = attemptService.getMyAttempts();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

