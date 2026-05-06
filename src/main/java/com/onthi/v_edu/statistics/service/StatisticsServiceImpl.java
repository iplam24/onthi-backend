package com.onthi.v_edu.statistics.service;

import com.onthi.v_edu.attempt.dto.AttemptFilterRequest;
import com.onthi.v_edu.attempt.entity.Answer;
import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.attempt.repository.AnswerRepository;
import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.common.constant.AttemptStatus;
import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.exam.entity.Exam;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.learning.entity.Subject;
import com.onthi.v_edu.learning.entity.Topic;
import com.onthi.v_edu.learning.repository.LevelRepository;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import com.onthi.v_edu.learning.repository.TopicRepository;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.repository.QuestionRepository;
import com.onthi.v_edu.statistics.dto.DashboardStatsResponse;
import com.onthi.v_edu.statistics.dto.DifficultyEvaluationResponse;
import com.onthi.v_edu.statistics.dto.StudentEvaluationResponse;
import com.onthi.v_edu.statistics.dto.SubjectEvaluationResponse;
import com.onthi.v_edu.statistics.dto.TopicEvaluationResponse;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.entity.UserInformation;
import com.onthi.v_edu.user.entity.UserStudyStreak;
import com.onthi.v_edu.user.repository.UserInformationRepository;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.user.repository.UserStudyStreakRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final UserRepository userRepository;
    private final UserInformationRepository userInformationRepository;
    private final UserStudyStreakRepository userStudyStreakRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;
    private final TopicRepository topicRepository;

    public StatisticsServiceImpl(UserRepository userRepository,
                                 UserInformationRepository userInformationRepository,
                                 UserStudyStreakRepository userStudyStreakRepository,
                                 QuestionRepository questionRepository,
                                 ExamRepository examRepository,
                                 AttemptRepository attemptRepository,
                                 AnswerRepository answerRepository,
                                 SubjectRepository subjectRepository,
                                 LevelRepository levelRepository,
                                 TopicRepository topicRepository) {
        this.userRepository = userRepository;
        this.userInformationRepository = userInformationRepository;
        this.userStudyStreakRepository = userStudyStreakRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
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

    @Override
    public ApiResponse<StudentEvaluationResponse> getMyStudentEvaluation(AttemptFilterRequest filter) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem đánh giá học sinh!");
        }

        AttemptFilterRequest effectiveFilter = filter == null ? new AttemptFilterRequest() : filter;
        String keyword = normalize(effectiveFilter.getKeyword());
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        UserInformation userInformation = userInformationRepository.findByUser_Id(currentUser.getId()).orElse(null);
        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(currentUser.getId()).orElse(null);

        Page<Attempt> attemptPage = attemptRepository.searchMyAttempts(
                currentUser.getId(),
                effectiveFilter.getSubjectId(),
                effectiveFilter.getLevelId(),
                effectiveFilter.getExamId(),
                effectiveFilter.getStatus(),
                effectiveFilter.getFlagged(),
                effectiveFilter.getFrom(),
                effectiveFilter.getTo(),
                keyword,
                Pageable.unpaged());

        List<Attempt> attempts = new ArrayList<>(attemptPage.getContent());
        attempts.sort(Comparator
                .comparing(Attempt::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Attempt::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        if (attempts.isEmpty()) {
            return new ApiResponse<>(HttpStatus.OK.value(), "Lấy đánh giá học sinh thành công!", buildEmptyEvaluation(currentUser, userInformation));
        }

        Map<Integer, SubjectStats> subjectStatsMap = new HashMap<>();
        Map<Integer, TopicStats> topicStatsMap = new HashMap<>();
        Map<DifficultyLevel, DifficultyStats> difficultyStatsMap = new EnumMap<>(DifficultyLevel.class);
        List<Double> attemptScores = new ArrayList<>();
        List<Double> speedScores = new ArrayList<>();
        Set<LocalDate> activeDaysLast30 = new HashSet<>();

        long totalAnswers = 0;
        long totalCorrectAnswers = 0;
        long totalCompletedAttempts = 0;
        double totalDurationSeconds = 0;
        long durationCount = 0;

        LocalDate cutoff = LocalDate.now().minusDays(29);

        for (Attempt attempt : attempts) {
            double attemptScore = safeDouble(attempt.getScore());
            attemptScores.add(attemptScore);

            if (attempt.getStartedAt() != null && !attempt.getStartedAt().toLocalDate().isBefore(cutoff)) {
                activeDaysLast30.add(attempt.getStartedAt().toLocalDate());
            }

            updateSubjectStats(subjectStatsMap, attempt, attemptScore);

            if (attempt.getStatus() != AttemptStatus.DOING) {
                totalCompletedAttempts++;
                totalCorrectAnswers += safeLong(attempt.getCorrectCount());
                totalAnswers += safeLong(attempt.getCorrectCount()) + safeLong(attempt.getWrongCount());

                Double speedScore = calculateSpeedScore(attempt);
                if (speedScore != null) {
                    speedScores.add(speedScore);
                }

                if (attempt.getDurationTaken() != null) {
                    totalDurationSeconds += attempt.getDurationTaken();
                    durationCount++;
                }
            }

            List<Answer> answers = answerRepository.findByAttemptIdWithDetails(attempt.getId());
            for (Answer answer : answers) {
                Question question = answer.getQuestion();
                if (question == null) {
                    continue;
                }

                Topic topic = question.getTopic();
                Subject subject = topic != null ? topic.getSubject() : null;
                updateTopicStats(topicStatsMap, topic, subject, answer);
                updateDifficultyStats(difficultyStatsMap, question.getDifficulty(), answer);
            }
        }

        double averageScore = average(attemptScores);
        double bestScore = attemptScores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double latestScore = attemptScores.getLast();
        double accuracyRate = totalAnswers == 0 ? 0 : (totalCorrectAnswers * 100.0 / totalAnswers);
        double averageDurationSeconds = durationCount == 0 ? 0 : totalDurationSeconds / durationCount;

        List<SubjectEvaluationResponse> subjectEvaluations = subjectStatsMap.values().stream()
                .map(this::toSubjectEvaluationResponse)
                .sorted(Comparator.comparingDouble(SubjectEvaluationResponse::getAverageScore).reversed())
                .toList();

        List<TopicEvaluationResponse> topicEvaluations = topicStatsMap.values().stream()
                .map(this::toTopicEvaluationResponse)
                .sorted(Comparator.comparingDouble(TopicEvaluationResponse::getAccuracyRate))
                .limit(10)
                .toList();

        List<DifficultyEvaluationResponse> difficultyEvaluations = buildDifficultyEvaluations(difficultyStatsMap);

        double knowledgeScore = clampToPercent(
                (averageScore * 0.55)
                        + (accuracyRate * 0.25)
                        + (weightedDifficultyScore(difficultyEvaluations) * 0.20));
        double speedScore = average(speedScores);
        double progressScore = calculateProgressScore(attemptScores);
        double disciplineScore = calculateDisciplineScore(streak, activeDaysLast30);
        double overallScore = clampToPercent(
                (knowledgeScore * 0.50)
                        + (speedScore * 0.15)
                        + (progressScore * 0.20)
                        + (disciplineScore * 0.15));

        SubjectEvaluationResponse strongestSubject = subjectEvaluations.isEmpty() ? null : subjectEvaluations.getFirst();
        SubjectEvaluationResponse weakestSubject = subjectEvaluations.isEmpty() ? null : subjectEvaluations.getLast();
        TopicEvaluationResponse weakestTopic = topicEvaluations.isEmpty() ? null : topicEvaluations.getFirst();

        List<String> strengths = buildStrengths(knowledgeScore, speedScore, progressScore, disciplineScore, strongestSubject);
        List<String> weaknesses = buildWeaknesses(knowledgeScore, speedScore, progressScore, disciplineScore, weakestSubject, weakestTopic, difficultyEvaluations);
        List<String> recommendations = buildRecommendations(speedScore, progressScore, disciplineScore, weakestSubject, weakestTopic, difficultyEvaluations);

        String performanceLabel = labelForScore(overallScore);
        String summary = buildSummary(currentUser, strongestSubject, weakestSubject, weakestTopic, overallScore, knowledgeScore, speedScore, progressScore, disciplineScore);

        StudentEvaluationResponse response = new StudentEvaluationResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                userInformation != null ? userInformation.getFullName() : null,
                userInformation != null ? userInformation.getSchoolName() : null,
                userInformation != null && userInformation.getLevel() != null ? userInformation.getLevel().getId() : null,
                userInformation != null && userInformation.getLevel() != null ? userInformation.getLevel().getName() : null,
                userInformation != null ? userInformation.getDob() : null,
                attempts.size(),
                totalCompletedAttempts,
                totalAnswers,
                round2(averageScore),
                round2(bestScore),
                round2(latestScore),
                round2(accuracyRate),
                round2(averageDurationSeconds),
                round2(knowledgeScore),
                round2(speedScore),
                round2(progressScore),
                round2(disciplineScore),
                round2(overallScore),
                performanceLabel,
                summary,
                strengths,
                weaknesses,
                recommendations,
                subjectEvaluations,
                topicEvaluations,
                difficultyEvaluations);

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy đánh giá học sinh thành công!", response);
    }

    private StudentEvaluationResponse buildEmptyEvaluation(User user, UserInformation userInformation) {
        return new StudentEvaluationResponse(
                user.getId(),
                user.getUsername(),
                userInformation != null ? userInformation.getFullName() : null,
                userInformation != null ? userInformation.getSchoolName() : null,
                userInformation != null && userInformation.getLevel() != null ? userInformation.getLevel().getId() : null,
                userInformation != null && userInformation.getLevel() != null ? userInformation.getLevel().getName() : null,
                userInformation != null ? userInformation.getDob() : null,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "Chưa đủ dữ liệu",
                "Bạn chưa có đủ lịch sử làm bài để đánh giá chi tiết.",
                List.of("Hãy làm vài đề gần nhất để hệ thống phân tích chính xác hơn."),
                List.of("Chưa đủ dữ liệu để xác định điểm yếu cụ thể."),
                List.of("Bắt đầu với đề cùng môn, cùng mức độ để tạo dữ liệu đầu vào.", "Giữ lửa ôn thi mỗi ngày để có dữ liệu tiến bộ."),
                List.of(),
                List.of(),
                List.of());
    }

    private void updateSubjectStats(Map<Integer, SubjectStats> subjectStatsMap, Attempt attempt, double attemptScore) {
        Exam exam = attempt.getExam();
        Subject subject = exam != null ? exam.getSubject() : null;
        if (subject == null) {
            return;
        }

        SubjectStats stats = subjectStatsMap.computeIfAbsent(subject.getId(), ignored -> new SubjectStats());
        stats.subjectId = subject.getId();
        stats.subjectName = subject.getName();
        stats.levelId = subject.getLevel() != null ? subject.getLevel().getId() : null;
        stats.levelName = subject.getLevel() != null ? subject.getLevel().getName() : null;
        stats.attemptCount++;
        stats.scoreSum += attemptScore;
        stats.bestScore = Math.max(stats.bestScore, attemptScore);
        stats.latestScore = attemptScore;
        stats.correctAnswers += safeLong(attempt.getCorrectCount());
        stats.totalAnswers += safeLong(attempt.getCorrectCount()) + safeLong(attempt.getWrongCount());
        if (attempt.getDurationTaken() != null && attempt.getStatus() != AttemptStatus.DOING) {
            stats.durationSum += attempt.getDurationTaken();
            stats.durationCount++;
        }
        if (attempt.getStatus() != AttemptStatus.DOING) {
            stats.completedCount++;
        }
    }

    private void updateTopicStats(Map<Integer, TopicStats> topicStatsMap, Topic topic, Subject subject, Answer answer) {
        if (topic == null) {
            return;
        }

        TopicStats stats = topicStatsMap.computeIfAbsent(topic.getId(), ignored -> new TopicStats());
        stats.topicId = topic.getId();
        stats.topicName = topic.getName();
        stats.subjectId = subject != null ? subject.getId() : null;
        stats.subjectName = subject != null ? subject.getName() : null;
        stats.totalAnswers++;
        if (Boolean.TRUE.equals(answer.getIsCorrect())) {
            stats.correctAnswers++;
        }
        stats.scoreSum += safeDouble(answer.getScore());
    }

    private void updateDifficultyStats(Map<DifficultyLevel, DifficultyStats> difficultyStatsMap,
                                       DifficultyLevel difficulty,
                                       Answer answer) {
        if (difficulty == null) {
            return;
        }

        DifficultyStats stats = difficultyStatsMap.computeIfAbsent(difficulty, ignored -> new DifficultyStats());
        stats.difficulty = difficulty;
        stats.totalAnswers++;
        if (Boolean.TRUE.equals(answer.getIsCorrect())) {
            stats.correctAnswers++;
        }
        stats.scoreSum += safeDouble(answer.getScore());
    }

    private SubjectEvaluationResponse toSubjectEvaluationResponse(SubjectStats stats) {
        double accuracyRate = stats.totalAnswers == 0 ? 0 : (stats.correctAnswers * 100.0 / stats.totalAnswers);
        double averageScore = stats.attemptCount == 0 ? 0 : stats.scoreSum / stats.attemptCount;
        double averageDurationSeconds = stats.durationCount == 0 ? 0 : stats.durationSum / stats.durationCount;
        return new SubjectEvaluationResponse(
                stats.subjectId,
                stats.subjectName,
                stats.levelId,
                stats.levelName,
                stats.attemptCount,
                round2(averageScore),
                round2(accuracyRate),
                round2(stats.bestScore),
                round2(stats.latestScore),
                round2(averageDurationSeconds)
        );
    }

    private TopicEvaluationResponse toTopicEvaluationResponse(TopicStats stats) {
        double accuracyRate = stats.totalAnswers == 0 ? 0 : (stats.correctAnswers * 100.0 / stats.totalAnswers);
        double averageScore = stats.totalAnswers == 0 ? 0 : stats.scoreSum / stats.totalAnswers;
        return new TopicEvaluationResponse(
                stats.topicId,
                stats.topicName,
                stats.subjectId,
                stats.subjectName,
                stats.totalAnswers,
                stats.correctAnswers,
                round2(accuracyRate),
                round2(averageScore)
        );
    }

    private List<DifficultyEvaluationResponse> buildDifficultyEvaluations(Map<DifficultyLevel, DifficultyStats> difficultyStatsMap) {
        List<DifficultyEvaluationResponse> responses = new ArrayList<>();
        for (DifficultyLevel difficulty : List.of(DifficultyLevel.EASY, DifficultyLevel.MEDIUM, DifficultyLevel.HARD)) {
            DifficultyStats stats = difficultyStatsMap.get(difficulty);
            if (stats == null) {
                continue;
            }

            double accuracyRate = stats.totalAnswers == 0 ? 0 : (stats.correctAnswers * 100.0 / stats.totalAnswers);
            double averageScore = stats.totalAnswers == 0 ? 0 : stats.scoreSum / stats.totalAnswers;
            responses.add(new DifficultyEvaluationResponse(
                    difficulty.name(),
                    stats.totalAnswers,
                    stats.correctAnswers,
                    round2(accuracyRate),
                    round2(averageScore)));
        }
        return responses;
    }

    private Double calculateSpeedScore(Attempt attempt) {
        Exam exam = attempt.getExam();
        if (exam == null || exam.getDuration() == null || exam.getDuration() <= 0 || attempt.getDurationTaken() == null) {
            return null;
        }

        double allowedSeconds = exam.getDuration() * 60.0;
        double ratio = attempt.getDurationTaken() / allowedSeconds;
        return clampToPercent(100 - (ratio * 100.0));
    }

    private double calculateProgressScore(List<Double> scores) {
        if (scores.isEmpty()) {
            return 0;
        }
        if (scores.size() == 1) {
            return clampToPercent(scores.getFirst());
        }

        double recent;
        double previous;
        if (scores.size() >= 10) {
            int size = scores.size();
            recent = average(scores.subList(size - 5, size));
            previous = average(scores.subList(size - 10, size - 5));
        } else {
            int middle = scores.size() / 2;
            previous = average(scores.subList(0, middle));
            recent = average(scores.subList(middle, scores.size()));
        }

        return clampToPercent(50 + (recent - previous));
    }

    private double calculateDisciplineScore(UserStudyStreak streak, Set<LocalDate> activeDaysLast30) {
        int currentStreak = streak != null && streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0;
        double streakScore = clampToPercent(currentStreak * 10.0);
        double consistencyScore = clampToPercent(activeDaysLast30.size() * 7.0);
        return clampToPercent((streakScore * 0.65) + (consistencyScore * 0.35));
    }

    private double weightedDifficultyScore(List<DifficultyEvaluationResponse> difficultyEvaluations) {
        if (difficultyEvaluations.isEmpty()) {
            return 0;
        }

        double weighted = 0;
        double weightSum = 0;
        for (DifficultyEvaluationResponse evaluation : difficultyEvaluations) {
            double weight = switch (evaluation.getDifficulty()) {
                case "EASY" -> 0.2;
                case "MEDIUM" -> 0.3;
                case "HARD" -> 0.5;
                default -> 0.2;
            };
            weighted += evaluation.getAccuracyRate() * weight;
            weightSum += weight;
        }
        return weightSum == 0 ? 0 : weighted / weightSum;
    }

    private List<String> buildStrengths(double knowledgeScore,
                                       double speedScore,
                                       double progressScore,
                                       double disciplineScore,
                                       SubjectEvaluationResponse strongestSubject) {
        Set<String> result = new LinkedHashSet<>();
        if (knowledgeScore >= 75) {
            result.add("Nền kiến thức đang khá tốt và xử lý được phần lớn câu hỏi cơ bản.");
        }
        if (strongestSubject != null && strongestSubject.getAverageScore() >= 75) {
            result.add("Mạnh nhất ở môn " + strongestSubject.getSubjectName() + ".");
        }
        if (speedScore >= 70) {
            result.add("Tốc độ làm bài đang ổn, ít bị mất điểm vì quá chậm.");
        }
        if (progressScore >= 70) {
            result.add("Bạn đang có xu hướng tiến bộ rõ ràng qua các đề gần đây.");
        }
        if (disciplineScore >= 70) {
            result.add("Thói quen học tập tương đối đều, giữ được nhịp ôn thi tốt.");
        }
        if (result.isEmpty()) {
            result.add("Bạn có một số điểm sáng, nhưng chưa đủ dữ liệu nổi bật để kết luận mạnh.");
        }
        return new ArrayList<>(result);
    }

    private List<String> buildWeaknesses(double knowledgeScore,
                                       double speedScore,
                                       double progressScore,
                                       double disciplineScore,
                                       SubjectEvaluationResponse weakestSubject,
                                       TopicEvaluationResponse weakestTopic,
                                       List<DifficultyEvaluationResponse> difficultyEvaluations) {
        Set<String> result = new LinkedHashSet<>();
        if (knowledgeScore < 60) {
            result.add("Bạn đang thiếu chắc kiến thức nền ở một số phần quan trọng.");
        }
        if (weakestSubject != null && weakestSubject.getAverageScore() < 60) {
            result.add("Bạn đang yếu hơn ở môn " + weakestSubject.getSubjectName() + ".");
        }
        if (weakestTopic != null && weakestTopic.getAccuracyRate() < 60) {
            result.add("Chủ đề " + weakestTopic.getTopicName() + " là điểm yếu rõ nhất hiện tại.");
        }
        DifficultyEvaluationResponse hard = difficultyEvaluations.stream()
                .filter(item -> "HARD".equals(item.getDifficulty()))
                .findFirst()
                .orElse(null);
        if (hard != null && hard.getAccuracyRate() < 50) {
            result.add("Câu khó / vận dụng cao còn yếu, cần luyện thêm sau khi chắc câu cơ bản.");
        }
        if (speedScore < 60) {
            result.add("Tốc độ làm bài còn chậm, dễ mất điểm ở cuối đề.");
        }
        if (disciplineScore < 60) {
            result.add("Nhịp học chưa đều, cần giữ streak ổn định hơn.");
        }
        if (progressScore < 50) {
            result.add("Kết quả gần đây chưa cho thấy tiến bộ rõ rệt.");
        }
        if (result.isEmpty()) {
            result.add("Chưa có điểm yếu nổi bật, nhưng vẫn nên duy trì ôn tập đều để giữ phong độ.");
        }
        return new ArrayList<>(result);
    }

    private List<String> buildRecommendations(double speedScore,
                                              double progressScore,
                                              double disciplineScore,
                                              SubjectEvaluationResponse weakestSubject,
                                              TopicEvaluationResponse weakestTopic,
                                              List<DifficultyEvaluationResponse> difficultyEvaluations) {
        Set<String> result = new LinkedHashSet<>();
        if (weakestSubject != null) {
            result.add("Ôn tập lại môn " + weakestSubject.getSubjectName() + " bằng đề ngắn và câu cơ bản trước.");
        }
        if (weakestTopic != null) {
            result.add("Luyện riêng chủ đề " + weakestTopic.getTopicName() + " để kéo tỷ lệ đúng lên.");
        }
        if (difficultyEvaluations.stream().anyMatch(item -> "HARD".equals(item.getDifficulty()) && item.getAccuracyRate() < 50)) {
            result.add("Chỉ tăng độ khó sau khi phần dễ và trung bình đã ổn.");
        }
        if (speedScore < 60) {
            result.add("Làm bài bấm giờ để cải thiện tốc độ và khả năng phân bổ thời gian.");
        }
        if (progressScore < 60) {
            result.add("So sánh 5 bài gần nhất để tìm đúng dạng sai lặp lại và sửa dứt điểm.");
        }
        if (disciplineScore < 60) {
            result.add("Giữ chuỗi ôn thi hằng ngày để hệ thống có dữ liệu tiến bộ ổn định hơn.");
        }
        if (result.isEmpty()) {
            result.add("Tiếp tục làm đề đều để giữ phong độ và mở rộng sang câu khó hơn.");
        }
        return new ArrayList<>(result);
    }

    private String buildSummary(User user,
                                SubjectEvaluationResponse strongestSubject,
                                SubjectEvaluationResponse weakestSubject,
                                TopicEvaluationResponse weakestTopic,
                                double overallScore,
                                double knowledgeScore,
                                double speedScore,
                                double progressScore,
                                double disciplineScore) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hồ sơ học tập của ").append(user.getUsername()).append(": ");
        if (strongestSubject != null) {
            builder.append("mạnh hơn ở môn ").append(strongestSubject.getSubjectName()).append(", ");
        }
        if (weakestSubject != null && weakestSubject.getAverageScore() < 60) {
            builder.append("cần cải thiện môn ").append(weakestSubject.getSubjectName()).append(", ");
        }
        if (weakestTopic != null) {
            builder.append("yếu rõ ở chủ đề ").append(weakestTopic.getTopicName()).append(", ");
        }
        builder.append("điểm tổng hợp hiện tại là ").append(round2(overallScore)).append("/100.");
        if (knowledgeScore < 60) {
            builder.append(" Nền kiến thức cần củng cố thêm.");
        }
        if (speedScore < 60) {
            builder.append(" Tốc độ làm bài còn cần cải thiện.");
        }
        if (progressScore >= 70) {
            builder.append(" Bạn đang có xu hướng tiến bộ.");
        }
        if (disciplineScore < 60) {
            builder.append(" Thói quen ôn tập nên đều hơn.");
        }
        return builder.toString();
    }

    private String labelForScore(double score) {
        if (score >= 90) {
            return "Xuất sắc";
        }
        if (score >= 75) {
            return "Tốt";
        }
        if (score >= 60) {
            return "Khá";
        }
        if (score >= 40) {
            return "Cần cải thiện";
        }
        return "Cảnh báo";
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double clampToPercent(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private long safeLong(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

        return null;
    }

    private static class SubjectStats {
        Integer subjectId;
        String subjectName;
        Integer levelId;
        String levelName;
        long attemptCount;
        long completedCount;
        long correctAnswers;
        long totalAnswers;
        double scoreSum;
        double bestScore;
        double latestScore;
        double durationSum;
        long durationCount;
    }

    private static class TopicStats {
        Integer topicId;
        String topicName;
        Integer subjectId;
        String subjectName;
        long totalAnswers;
        long correctAnswers;
        double scoreSum;
    }

    private static class DifficultyStats {
        DifficultyLevel difficulty;
        long totalAnswers;
        long correctAnswers;
        double scoreSum;
    }
}
