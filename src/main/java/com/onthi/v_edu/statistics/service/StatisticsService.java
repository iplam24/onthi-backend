package com.onthi.v_edu.statistics.service;

import com.onthi.v_edu.attempt.dto.AttemptFilterRequest;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.statistics.dto.DashboardStatsResponse;
import com.onthi.v_edu.statistics.dto.StudentEvaluationResponse;

public interface StatisticsService {
    ApiResponse<DashboardStatsResponse> getDashboardStats();

    ApiResponse<StudentEvaluationResponse> getMyStudentEvaluation(AttemptFilterRequest filter);
}
