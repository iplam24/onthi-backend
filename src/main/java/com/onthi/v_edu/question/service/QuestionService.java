package com.onthi.v_edu.question.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.question.dto.QuestionRequest;
import com.onthi.v_edu.question.dto.QuestionResponse;

import java.util.List;

public interface QuestionService {


	ApiResponse<List<QuestionResponse>> getAllQuestions();

	ApiResponse<QuestionResponse> getQuestionById(Integer id);

	ApiResponse<QuestionResponse> createQuestion(QuestionRequest request);

	ApiResponse<QuestionResponse> updateQuestion(Integer id, QuestionRequest request);

	ApiResponse<Void> deleteQuestion(Integer id);
}
