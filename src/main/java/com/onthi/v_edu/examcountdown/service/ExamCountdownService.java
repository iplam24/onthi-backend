package com.onthi.v_edu.examcountdown.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.examcountdown.dto.ExamCountdownRequest;
import com.onthi.v_edu.examcountdown.dto.ExamCountdownResponse;

import java.util.List;

public interface ExamCountdownService {
    ApiResponse<List<ExamCountdownResponse>> getCountdownForCurrentUser();
    ApiResponse<ExamCountdownResponse> create(ExamCountdownRequest request);
    ApiResponse<ExamCountdownResponse> update(Integer id, ExamCountdownRequest request);
    ApiResponse<Void> delete(Integer id);
}
