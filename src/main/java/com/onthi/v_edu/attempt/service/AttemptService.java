package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.attempt.dto.AttemptDetailResponse;
import com.onthi.v_edu.attempt.dto.AttemptStartRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitRequest;
import com.onthi.v_edu.attempt.dto.AttemptSummaryResponse;
import com.onthi.v_edu.common.dto.ApiResponse;

import java.util.List;

public interface AttemptService {

	ApiResponse<AttemptDetailResponse> startAttempt(AttemptStartRequest request);

	ApiResponse<AttemptDetailResponse> submitAttempt(Integer attemptId, AttemptSubmitRequest request);

	ApiResponse<AttemptDetailResponse> getMyAttemptById(Integer attemptId);

	ApiResponse<List<AttemptSummaryResponse>> getMyAttempts();
}

