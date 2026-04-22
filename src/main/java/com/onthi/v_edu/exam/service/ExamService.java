package com.onthi.v_edu.exam.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.dto.ExamResponse;
import org.springframework.data.domain.Pageable;

public interface ExamService {

	ApiResponse<PageResponse<ExamResponse>> getAllExams(Pageable pageable);

	ApiResponse<PageResponse<ExamResponse>> getExamsBySubjectId(Integer subjectId, Pageable pageable);

	ApiResponse<ExamResponse> getExamById(Integer id);

	ApiResponse<ExamResponse> createExam(ExamRequest request);

	ApiResponse<ExamResponse> updateExam(Integer id, ExamRequest request);

	ApiResponse<Void> deleteExam(Integer id);
}
