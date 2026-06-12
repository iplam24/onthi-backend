package com.onthi.v_edu.common.exception;

import com.onthi.v_edu.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .reduce((m1, m2) -> m1 + ", " + m2)
                .orElse("Dữ liệu đầu vào không hợp lệ");
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        String message = "Dữ liệu JSON gửi lên không đúng định dạng hoặc sai kiểu dữ liệu.";
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
            message = "Sai kiểu dữ liệu đầu vào. Vui lòng kiểm tra lại.";
        }
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Thiếu tham số bắt buộc: " + ex.getParameterName());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Tham số '" + ex.getName() + "' sai kiểu dữ liệu.");
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.FORBIDDEN.value(), "Bạn không có quyền truy cập tài nguyên này");
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        ex.printStackTrace(); // Log lỗi ra console để debug
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Đã xảy ra lỗi hệ thống");
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

