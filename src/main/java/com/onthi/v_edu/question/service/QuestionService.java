package com.onthi.v_edu.question.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.question.dto.QuestionRequest;
import com.onthi.v_edu.question.dto.QuestionResponse;
import com.onthi.v_edu.question.dto.QuestionGroupRequest;
import com.onthi.v_edu.question.dto.QuestionGroupResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface QuestionService {

	ApiResponse<PageResponse<QuestionResponse>> getAllQuestions(Integer subjectId, Integer topicId, Pageable pageable);

	ApiResponse<QuestionResponse> getQuestionById(Integer id);

	ApiResponse<QuestionResponse> createQuestion(QuestionRequest request);

	ApiResponse<QuestionResponse> updateQuestion(Integer id, QuestionRequest request);

	ApiResponse<Void> deleteQuestion(Integer id);

	ApiResponse<Void> createQuestions(List<QuestionRequest> requests);
	
	ApiResponse<QuestionGroupResponse> createQuestionGroup(QuestionGroupRequest request);

	ApiResponse<QuestionGroupResponse> getQuestionGroupById(Integer id);

	ApiResponse<QuestionGroupResponse> updateQuestionGroup(Integer id, QuestionGroupRequest request);

	ApiResponse<Void> deleteQuestionGroup(Integer id);

	ApiResponse<PageResponse<QuestionGroupResponse>> getAllQuestionGroups(Integer subjectId, Integer topicId, Pageable pageable);

	ApiResponse<Void> importQuestionsFromExcel(org.springframework.web.multipart.MultipartFile file,
			String imageFolderPath);

	ApiResponse<List<QuestionRequest>> previewQuestionsFromExcel(org.springframework.web.multipart.MultipartFile file,
			String imageFolderPath);

	org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> generateExcelTemplate();
}
