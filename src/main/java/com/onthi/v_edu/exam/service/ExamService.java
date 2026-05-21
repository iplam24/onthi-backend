package com.onthi.v_edu.exam.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.exam.dto.ExamPerformanceResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.dto.ExamResponse;
import com.onthi.v_edu.exam.dto.RandomExamRequest;
import com.onthi.v_edu.exam.dto.RandomExamResponse;
import com.onthi.v_edu.exam.dto.UserExamHistoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ExamService {

	ApiResponse<PageResponse<ExamResponse>> getAllExams(Pageable pageable);

	ApiResponse<PageResponse<ExamResponse>> getExamsBySubjectId(Integer subjectId, Pageable pageable);

	ApiResponse<ExamResponse> getExamById(Integer id);

	ApiResponse<ExamResponse> createExam(ExamRequest request);

	ApiResponse<ExamResponse> updateExam(Integer id, ExamRequest request);

	ApiResponse<Void> deleteExam(Integer id);

	ApiResponse<RandomExamResponse> generateRandomExam(RandomExamRequest request);

	ApiResponse<PageResponse<UserExamHistoryResponse>> getMyExamHistory(Integer subjectId, Pageable pageable);

	ApiResponse<ExamPerformanceResponse> getAttemptPerformance(Integer attemptId);

	ApiResponse<Map<String, Object>> checkRetakeEligibility(Integer examId);
}
