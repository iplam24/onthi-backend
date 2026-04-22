package com.onthi.v_edu.exam.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.dto.ExamResponse;

import java.util.List;

public interface ExamService {

	ApiResponse<List<ExamResponse>> getAllExams();

	ApiResponse<List<ExamResponse>> getExamsBySubjectId(Integer subjectId);

	ApiResponse<ExamResponse> getExamById(Integer id);

	ApiResponse<ExamResponse> createExam(ExamRequest request);

	ApiResponse<ExamResponse> updateExam(Integer id, ExamRequest request);

	ApiResponse<Void> deleteExam(Integer id);
}
