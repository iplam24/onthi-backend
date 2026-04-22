package com.onthi.v_edu.exam.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@RestController
@RequestMapping("/api/exams")
@PreAuthorize("isAuthenticated()")
public class ExamController {

	private final ExamService examService;

	public ExamController(ExamService examService) {
		this.examService = examService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<?>> getAllExams(
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		ApiResponse<?> response = examService.getAllExams(pageable);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/subjects/{subjectId}")
	public ResponseEntity<ApiResponse<?>> getExamsBySubjectId(
			@PathVariable Integer subjectId,
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		ApiResponse<?> response = examService.getExamsBySubjectId(subjectId, pageable);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<?>> getExamById(@PathVariable Integer id) {
		ApiResponse<?> response = examService.getExamById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createExam(@Valid @RequestBody ExamRequest request) {
		ApiResponse<?> response = examService.createExam(request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateExam(@PathVariable Integer id, @Valid @RequestBody ExamRequest request) {
		ApiResponse<?> response = examService.updateExam(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteExam(@PathVariable Integer id) {
		ApiResponse<?> response = examService.deleteExam(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
