package com.onthi.v_edu.learning.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.learning.service.LearningService;
import com.onthi.v_edu.learning.service.LearningService.LevelRequest;
import com.onthi.v_edu.learning.service.LearningService.SubjectRequest;
import com.onthi.v_edu.learning.service.LearningService.TopicRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/learning")
public class LearningController {

	private final LearningService learningService;

	public LearningController(LearningService learningService) {
		this.learningService = learningService;
	}

	@GetMapping("/levels")
	public ResponseEntity<ApiResponse<?>> getAllLevels() {
		ApiResponse<?> response = learningService.getAllLevels();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/levels/{id}")
	public ResponseEntity<ApiResponse<?>> getLevelById(@PathVariable Integer id) {
		ApiResponse<?> response = learningService.getLevelById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("/levels")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createLevel(@Valid @RequestBody LevelRequest request) {
		ApiResponse<?> response = learningService.createLevel(request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/levels/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateLevel(@PathVariable Integer id, @Valid @RequestBody LevelRequest request) {
		ApiResponse<?> response = learningService.updateLevel(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/levels/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteLevel(@PathVariable Integer id) {
		ApiResponse<?> response = learningService.deleteLevel(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/subjects")
	public ResponseEntity<ApiResponse<?>> getAllSubjects() {
		ApiResponse<?> response = learningService.getAllSubjects();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/subjects/{id}")
	public ResponseEntity<ApiResponse<?>> getSubjectById(@PathVariable Integer id) {
		ApiResponse<?> response = learningService.getSubjectById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("/subjects")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createSubject(@Valid @RequestBody SubjectRequest request) {
		ApiResponse<?> response = learningService.createSubject(request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/subjects/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateSubject(@PathVariable Integer id, @Valid @RequestBody SubjectRequest request) {
		ApiResponse<?> response = learningService.updateSubject(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/subjects/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteSubject(@PathVariable Integer id) {
		ApiResponse<?> response = learningService.deleteSubject(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/topics")
	public ResponseEntity<ApiResponse<?>> getAllTopics() {
		ApiResponse<?> response = learningService.getAllTopics();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/topics/{id}")
	public ResponseEntity<ApiResponse<?>> getTopicById(@PathVariable Integer id) {
		ApiResponse<?> response = learningService.getTopicById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("/topics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> createTopic(@Valid @RequestBody TopicRequest request) {
		ApiResponse<?> response = learningService.createTopic(request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/topics/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> updateTopic(@PathVariable Integer id, @Valid @RequestBody TopicRequest request) {
		ApiResponse<?> response = learningService.updateTopic(id, request);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/topics/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> deleteTopic(@PathVariable Integer id) {
		ApiResponse<?> response = learningService.deleteTopic(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
