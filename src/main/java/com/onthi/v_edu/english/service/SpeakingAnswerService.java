package com.onthi.v_edu.english.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse;
import com.onthi.v_edu.common.fileupload.service.FileUpLoadService;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
public class SpeakingAnswerService {

    private final FileUpLoadService fileUpLoadService;
    private final SubjectRepository subjectRepository;

    public SpeakingAnswerService(FileUpLoadService fileUpLoadService,
                                  SubjectRepository subjectRepository) {
        this.fileUpLoadService = fileUpLoadService;
        this.subjectRepository = subjectRepository;
    }

    /**
     * Upload audio blob (speaking answer) in the middle of an attempt.
     * Audio is saved via existing FileUpLoadService (same as image/other uploads).
     */
    public ApiResponse<UploadedFileResponse> uploadSpeakingAudio(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "File audio không được rỗng!");
        }
        String contentType = audioFile.getContentType();
        boolean isAudio = contentType != null && (
            contentType.startsWith("audio/") || 
            contentType.equals("video/webm") || 
            contentType.equals("video/mp4") || 
            contentType.equals("application/octet-stream")
        );
        if (!isAudio) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Chỉ hỗ trợ file audio! Mime type nhận được: " + contentType);
        }
        return fileUpLoadService.uploadFile(audioFile);
    }

    /**
     * Check whether an exam belongs to an English subject.
     */
    public boolean isEnglishExam(Integer examId, Integer subjectId) {
        if (subjectId != null) {
            return subjectRepository.findById(subjectId)
                    .map(s -> {
                        String name = s.getName() == null ? "" : s.getName().toLowerCase(Locale.ROOT);
                        return name.contains("tiếng anh") || name.contains("english");
                    })
                    .orElse(false);
        }
        return false;
    }
}
