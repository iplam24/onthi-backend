package com.onthi.v_edu.english.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse;
import com.onthi.v_edu.english.service.SpeakingAnswerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/english/speaking")
@PreAuthorize("isAuthenticated()")
public class SpeakingController {

    private final SpeakingAnswerService speakingAnswerService;

    public SpeakingController(SpeakingAnswerService speakingAnswerService) {
        this.speakingAnswerService = speakingAnswerService;
    }

    /**
     * POST /api/english/speaking/upload-audio
     * Upload speaking audio blob during an attempt.
     */
    @PostMapping(value = "/upload-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedFileResponse>> uploadAudio(@RequestPart("audioFile") MultipartFile audioFile) {
        ApiResponse<UploadedFileResponse> result = speakingAnswerService.uploadSpeakingAudio(audioFile);
        return ResponseEntity.status(result.getStatus()).body(result);
    }
}
