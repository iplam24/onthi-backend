package com.onthi.v_edu.statistics.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.statistics.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<?>> getDashboardStats() {
        ApiResponse<?> response = statisticsService.getDashboardStats();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
