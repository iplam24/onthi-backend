package com.onthi.v_edu.auth.controller;

import com.onthi.v_edu.auth.dto.LoginRequest;
import com.onthi.v_edu.auth.dto.SignUpRequest;
import com.onthi.v_edu.auth.service.AuthService;
import com.onthi.v_edu.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest loginRequest) {
		ApiResponse<?> response = authService.login(loginRequest);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody SignUpRequest signUpRequest) {
		ApiResponse<?> response = authService.register(signUpRequest);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
