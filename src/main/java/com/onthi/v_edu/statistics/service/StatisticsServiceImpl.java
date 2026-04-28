package com.onthi.v_edu.statistics.service;

import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.learning.repository.LevelRepository;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import com.onthi.v_edu.learning.repository.TopicRepository;
import com.onthi.v_edu.question.repository.QuestionRepository;
import com.onthi.v_edu.statistics.dto.DashboardStatsResponse;
import com.onthi.v_edu.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final AttemptRepository attemptRepository;
    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;
    private final TopicRepository topicRepository;

    public StatisticsServiceImpl(UserRepository userRepository,
                                 QuestionRepository questionRepository,
                                 ExamRepository examRepository,
                                 AttemptRepository attemptRepository,
                                 SubjectRepository subjectRepository,
                                 LevelRepository levelRepository,
                                 TopicRepository topicRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.subjectRepository = subjectRepository;
        this.levelRepository = levelRepository;
        this.topicRepository = topicRepository;
    }

    @Override
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        long totalUsers = userRepository.count();
        long newUsersToday = userRepository.countNewUsersSince(LocalDate.now().atStartOfDay());
        long totalQuestions = questionRepository.count();
        long totalExams = examRepository.count();
        long totalAttempts = attemptRepository.count();
        long totalSubjects = subjectRepository.count();
        long totalLevels = levelRepository.count();
        long totalTopics = topicRepository.count();

        DashboardStatsResponse stats = new DashboardStatsResponse(
                totalUsers,
                newUsersToday,
                totalQuestions,
                totalExams,
                totalAttempts,
                totalSubjects,
                totalLevels,
                totalTopics
        );

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy dữ liệu thống kê thành công!", stats);
    }
}
