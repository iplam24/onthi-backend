package com.onthi.v_edu.statistics.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.statistics.dto.DashboardStatsResponse;

public interface StatisticsService {
    ApiResponse<DashboardStatsResponse> getDashboardStats();
}
