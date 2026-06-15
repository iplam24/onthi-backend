package com.onthi.v_edu.question.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.question.dto.QuestionRequest;
import com.onthi.v_edu.question.service.QuestionService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("isAuthenticated()")
public class QuestionController {

	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<?>> getAllQuestions(
			@RequestParam(required = false) Integer subjectId,
			@RequestParam(required = false) Integer topicId,
			@ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		ApiResponse<?> response = questionService.getAllQuestions(subjectId, topicId, pageable);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/groups")
	public ResponseEntity<ApiResponse<?>> getAllQuestionGroups(
			@RequestParam(required = false) Integer subjectId,
			@RequestParam(required = false) Integer topicId,
			@ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		ApiResponse<?> response = questionService.getAllQuestionGroups(subjectId, topicId, pageable);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<?>> getQuestionById(@PathVariable Integer id) {
		ApiResponse<?> response = questionService.getQuestionById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createQuestion(@Valid @RequestBody QuestionRequest request) {
		ApiResponse<?> response = questionService.createQuestion(request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("/batch")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createBatchQuestions(@RequestBody List<QuestionRequest> requests) {
		ApiResponse<?> response = questionService.createQuestions(requests);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("/group")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createQuestionGroup(@Valid @RequestBody com.onthi.v_edu.question.dto.QuestionGroupRequest request) {
		ApiResponse<?> response = questionService.createQuestionGroup(request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/group/{id}")
	public ResponseEntity<ApiResponse<?>> getQuestionGroupById(@PathVariable Integer id) {
		ApiResponse<?> response = questionService.getQuestionGroupById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/group/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateQuestionGroup(@PathVariable Integer id,
			@Valid @RequestBody com.onthi.v_edu.question.dto.QuestionGroupRequest request) {
		ApiResponse<?> response = questionService.updateQuestionGroup(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/group/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteQuestionGroup(@PathVariable Integer id) {
		ApiResponse<?> response = questionService.deleteQuestionGroup(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateQuestion(@PathVariable Integer id,
			@Valid @RequestBody QuestionRequest request) {
		ApiResponse<?> response = questionService.updateQuestion(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteQuestion(@PathVariable Integer id) {
		ApiResponse<?> response = questionService.deleteQuestion(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> importQuestionsFromExcel(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "imageFolderPath", required = false) String imageFolderPath) {
		ApiResponse<?> response = questionService.importQuestionsFromExcel(file, imageFolderPath);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> previewQuestionsFromExcel(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "imageFolderPath", required = false) String imageFolderPath) {
		ApiResponse<?> response = questionService.previewQuestionsFromExcel(file, imageFolderPath);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/import/template")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Resource> downloadExcelTemplate() {
		return questionService.generateExcelTemplate();
	}
}
