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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;

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
			@ParameterObject
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		ApiResponse<?> response = questionService.getAllQuestions(pageable);
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

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateQuestion(@PathVariable Integer id, @Valid @RequestBody QuestionRequest request) {
		ApiResponse<?> response = questionService.updateQuestion(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteQuestion(@PathVariable Integer id) {
		ApiResponse<?> response = questionService.deleteQuestion(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
