package com.onthi.v_edu.examcountdown.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.examcountdown.dto.ExamCountdownRequest;
import com.onthi.v_edu.examcountdown.service.ExamCountdownService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/countdowns")
public class ExamCountdownController {

    private final ExamCountdownService examCountdownService;

    public ExamCountdownController(ExamCountdownService examCountdownService) {
        this.examCountdownService = examCountdownService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getCountdownForCurrentUser() {
        ApiResponse<?> response = examCountdownService.getCountdownForCurrentUser();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody ExamCountdownRequest request) {
        ApiResponse<?> response = examCountdownService.create(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Integer id, @Valid @RequestBody ExamCountdownRequest request) {
        ApiResponse<?> response = examCountdownService.update(id, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Integer id) {
        ApiResponse<?> response = examCountdownService.delete(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
