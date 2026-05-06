package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.attempt.dto.AttemptDetailResponse;
import com.onthi.v_edu.attempt.dto.AttemptFilterRequest;
import com.onthi.v_edu.attempt.dto.AttemptStartRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitRequest;
import com.onthi.v_edu.attempt.dto.AttemptSummaryResponse;
import com.onthi.v_edu.attempt.dto.ViolationRecordRequest;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AttemptService {

	ApiResponse<AttemptDetailResponse> startAttempt(AttemptStartRequest request);

	ApiResponse<AttemptDetailResponse> submitAttempt(Integer attemptId, AttemptSubmitRequest request);

	ApiResponse<AttemptDetailResponse> recordViolation(Integer attemptId, ViolationRecordRequest request);

	ApiResponse<AttemptDetailResponse> getMyAttemptById(Integer attemptId);

	ApiResponse<PageResponse<AttemptSummaryResponse>> getMyAttempts(AttemptFilterRequest filter, Pageable pageable);
}
