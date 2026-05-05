package com.onthi.v_edu.examcountdown.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.examcountdown.dto.ExamCountdownRequest;
import com.onthi.v_edu.examcountdown.dto.ExamCountdownResponse;
import com.onthi.v_edu.examcountdown.entity.ExamCountdown;
import com.onthi.v_edu.examcountdown.repository.ExamCountdownRepository;
import com.onthi.v_edu.learning.entity.Level;
import com.onthi.v_edu.learning.repository.LevelRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.entity.UserInformation;
import com.onthi.v_edu.user.repository.UserInformationRepository;
import com.onthi.v_edu.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamCountdownServiceImpl implements ExamCountdownService {

    private final ExamCountdownRepository examCountdownRepository;
    private final LevelRepository levelRepository;
    private final UserRepository userRepository;
    private final UserInformationRepository userInformationRepository;

    public ExamCountdownServiceImpl(ExamCountdownRepository examCountdownRepository,
                                    LevelRepository levelRepository,
                                    UserRepository userRepository,
                                    UserInformationRepository userInformationRepository) {
        this.examCountdownRepository = examCountdownRepository;
        this.levelRepository = levelRepository;
        this.userRepository = userRepository;
        this.userInformationRepository = userInformationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ExamCountdownResponse>> getCountdownForCurrentUser() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem lịch đếm ngược!");
        }

        UserInformation userInfo = userInformationRepository.findByUser_Id(currentUser.getId()).orElse(null);
        if (userInfo == null || userInfo.getLevel() == null) {
            return new ApiResponse<>(HttpStatus.OK.value(), "Không tìm thấy thông tin level của người dùng.", Collections.emptyList());
        }

        List<ExamCountdownResponse> data = examCountdownRepository.findByLevel_IdOrderByExamDateAsc(userInfo.getLevel().getId()).stream()
                .map(this::toExamCountdownResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy lịch đếm ngược thành công!", data);
    }

    @Override
    public ApiResponse<ExamCountdownResponse> create(ExamCountdownRequest request) {
        Level level = levelRepository.findById(request.getLevelId()).orElse(null);
        if (level == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
        }

        ExamCountdown countdown = new ExamCountdown();
        countdown.setTitle(request.getTitle());
        countdown.setExamDate(request.getExamDate());
        countdown.setLevel(level);
        countdown = examCountdownRepository.save(countdown);

        return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo lịch đếm ngược thành công!", toExamCountdownResponse(countdown));
    }

    @Override
    public ApiResponse<ExamCountdownResponse> update(Integer id, ExamCountdownRequest request) {
        ExamCountdown countdown = examCountdownRepository.findById(id).orElse(null);
        if (countdown == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lịch đếm ngược!");
        }

        Level level = levelRepository.findById(request.getLevelId()).orElse(null);
        if (level == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
        }

        countdown.setTitle(request.getTitle());
        countdown.setExamDate(request.getExamDate());
        countdown.setLevel(level);
        countdown = examCountdownRepository.save(countdown);

        return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật lịch đếm ngược thành công!", toExamCountdownResponse(countdown));
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        if (!examCountdownRepository.existsById(id)) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lịch đếm ngược!");
        }
        examCountdownRepository.deleteById(id);
        return new ApiResponse<>(HttpStatus.OK.value(), "Xoá lịch đếm ngược thành công!");
    }

    private ExamCountdownResponse toExamCountdownResponse(ExamCountdown countdown) {
        return new ExamCountdownResponse(
                countdown.getId(),
                countdown.getTitle(),
                countdown.getExamDate(),
                countdown.getLevel().getId(),
                countdown.getLevel().getName()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }

        String username = authentication.getName();
        if (isBlank(username)) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
