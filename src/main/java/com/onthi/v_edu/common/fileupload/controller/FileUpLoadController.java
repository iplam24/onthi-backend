package com.onthi.v_edu.common.fileupload.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse;
import com.onthi.v_edu.common.fileupload.service.FileUpLoadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@PreAuthorize("isAuthenticated()")
// Allow CORS from admin/frontend domains specifically for upload endpoint as a fallback
@org.springframework.web.bind.annotation.CrossOrigin(
		origins = {"https://admin.vuxuanlam.me", "https://onthi.vuxuanlam.me", "https://api.vuxuanlam.me"},
		allowCredentials = "true",
		allowedHeaders = "*",
		methods = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.OPTIONS}
)
public class FileUpLoadController {

	private final FileUpLoadService fileUpLoadService;

	public FileUpLoadController(FileUpLoadService fileUpLoadService) {
		this.fileUpLoadService = fileUpLoadService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<UploadedFileResponse>> uploadFile(@RequestParam("file") MultipartFile file) {
		ApiResponse<UploadedFileResponse> response = fileUpLoadService.uploadFile(file);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
