package com.onthi.v_edu.auth.service;

import com.onthi.v_edu.auth.dto.LoginRequest;
import com.onthi.v_edu.auth.dto.SignUpRequest;
import com.onthi.v_edu.common.dto.ApiResponse;

public interface AuthService {

    ApiResponse<?> login(LoginRequest loginRequest);

    ApiResponse<?> register(SignUpRequest signUpRequest);

}
