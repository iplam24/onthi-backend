package com.onthi.v_edu.english.controller;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.english.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/english/tts")
@RequiredArgsConstructor
@Slf4j
public class TtsController {

    private final TtsService ttsService;

    @Value("${app.file-upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file-upload-base-url:/uploads}")
    private String uploadBaseUrl;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateSpeech(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String voice = request.get("voice"); // alloy, echo, fable, onyx, nova, shimmer

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Văn bản không được để trống!"));
        }

        byte[] audioBytes = ttsService.generateSpeech(text, voice);
        if (audioBytes == null || audioBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Không thể tạo âm thanh AI từ văn bản này. Hãy kiểm tra lại API Key Custom OpenAI."));
        }

        try {
            // Build date folder structure matching existing FileUploadService
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetDir = uploadRootPath.resolve(dateFolder).normalize();
            Files.createDirectories(targetDir);

            String fileName = "tts-" + UUID.randomUUID().toString() + ".mp3";
            Path targetPath = targetDir.resolve(fileName).normalize();
            Files.write(targetPath, audioBytes);

            String relativeStoredPath = dateFolder + "/" + fileName;
            String publicUrl = uploadBaseUrl + "/" + relativeStoredPath;
            // Clean up double slashes
            publicUrl = publicUrl.replaceAll("(?<!http:)/+", "/");

            log.info("[TTS CONTROLLER] TTS Audio saved to: {} | URL: {}", targetPath, publicUrl);

            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Tạo giọng nói AI thành công!", Map.of("url", publicUrl)));
        } catch (IOException e) {
            log.error("[TTS CONTROLLER] Error saving generated speech file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi hệ thống khi lưu trữ file âm thanh: " + e.getMessage()));
        }
    }
}
