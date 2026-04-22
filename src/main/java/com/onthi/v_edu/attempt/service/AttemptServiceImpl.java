package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.attempt.dto.AttemptAnswerResponse;
import com.onthi.v_edu.attempt.dto.AttemptDetailResponse;
import com.onthi.v_edu.attempt.dto.AttemptStartRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitAnswerRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitRequest;
import com.onthi.v_edu.attempt.dto.AttemptSummaryResponse;
import com.onthi.v_edu.attempt.entity.Answer;
import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.attempt.repository.AnswerRepository;
import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.common.constant.AttemptStatus;
import com.onthi.v_edu.common.constant.QuestionType;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.exam.entity.Exam;
import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.repository.ExamQuestionRepository;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.question.entity.EssayAnswer;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.entity.QuestionOption;
import com.onthi.v_edu.question.repository.EssayAnswerRepository;
import com.onthi.v_edu.question.repository.QuestionOptionRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class AttemptServiceImpl implements AttemptService {

    private static final int TAB_SWITCH_FLAG_THRESHOLD = 5;
    private static final int VIOLATION_SCORE_FLAG_THRESHOLD = 50;

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final EssayAnswerRepository essayAnswerRepository;
    private final UserRepository userRepository;

    public AttemptServiceImpl(AttemptRepository attemptRepository,
                              AnswerRepository answerRepository,
                              ExamRepository examRepository,
                              ExamQuestionRepository examQuestionRepository,
                              QuestionOptionRepository questionOptionRepository,
                              EssayAnswerRepository essayAnswerRepository,
                              UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.essayAnswerRepository = essayAnswerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse<AttemptDetailResponse> startAttempt(AttemptStartRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để bắt đầu làm bài!");
        }

        Exam exam = examRepository.findById(request.getExamId()).orElse(null);
        if (exam == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy đề thi!");
        }

        String antiCheatError = validateStartConstraints(currentUser, exam);
        if (antiCheatError != null) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), antiCheatError);
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam_IdOrderByOrderIndexAscQuestion_IdAsc(exam.getId());
        if (examQuestions.isEmpty()) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Đề thi chưa có câu hỏi, không thể bắt đầu!");
        }

        Attempt attempt = new Attempt();
        attempt.setUser(currentUser);
        attempt.setExam(exam);
        attempt.setStatus(AttemptStatus.DOING);
        attempt.setScore(0d);
        attempt.setCorrectCount(0);
        attempt.setWrongCount(0);
        attempt.setTotalQuestions(examQuestions.size());
        attempt.setDurationTaken(0);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTabSwitchCount(0);
        attempt.setViolationScore(0);
        attempt.setFlagged(Boolean.FALSE);
        attempt = attemptRepository.save(attempt);

        return new ApiResponse<>(HttpStatus.CREATED.value(), "Bắt đầu làm bài thành công!", toAttemptDetailResponse(attempt));
    }

    @Override
    public ApiResponse<AttemptDetailResponse> submitAttempt(Integer attemptId, AttemptSubmitRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để nộp bài!");
        }

        Attempt attempt = attemptRepository.findByIdAndUser_Id(attemptId, currentUser.getId()).orElse(null);
        if (attempt == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lượt làm bài!");
        }

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Lượt làm bài này đã được nộp trước đó!");
        }
        if (attempt.getStatus() == AttemptStatus.EXPIRED) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Lượt làm bài này đã hết hạn!");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = calculateDeadline(attempt);
        if (deadline != null && now.isAfter(deadline)) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setExpiredAt(now);
            attempt.setDurationTaken(calculateDurationSeconds(attempt.getStartedAt(), now));
            attemptRepository.save(attempt);
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Đã hết thời gian làm bài, không thể nộp!");
        }

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExam_IdOrderByOrderIndexAscQuestion_IdAsc(attempt.getExam().getId());
        Map<Integer, ExamQuestion> examQuestionMap = new HashMap<>();
        for (ExamQuestion examQuestion : examQuestions) {
            Question question = examQuestion.getQuestion();
            if (question != null) {
                examQuestionMap.put(question.getId(), examQuestion);
            }
        }

        List<AttemptSubmitAnswerRequest> submittedAnswers = request.getAnswers() == null
                ? Collections.emptyList()
                : request.getAnswers();

        String antiCheatError = validateSubmissionPayload(submittedAnswers, examQuestionMap);
        if (antiCheatError != null) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), antiCheatError);
        }

        answerRepository.deleteByAttempt_Id(attempt.getId());

        double totalScore = 0d;
        int correctCount = 0;
        int wrongCount = 0;
        List<Answer> answersToSave = new ArrayList<>();

        for (AttemptSubmitAnswerRequest submitted : submittedAnswers) {
            ExamQuestion examQuestion = examQuestionMap.get(submitted.getQuestionId());
            if (examQuestion == null || examQuestion.getQuestion() == null) {
                continue;
            }

            Question question = examQuestion.getQuestion();
            double questionScore = examQuestion.getScore() == null ? 1d : examQuestion.getScore();

            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setEssayAnswer(normalize(submitted.getEssayAnswer()));
            answer.setQuestionSnapshot(question.getContent());
            answer.setCreatedAt(now);
            answer.setUpdatedAt(now);

            if (question.getType() == QuestionType.MCQ) {
                QuestionOption selectedOption = resolveSelectedOption(question.getId(), submitted.getSelectedOptionId());
                QuestionOption correctOption = questionOptionRepository.findFirstByQuestion_IdAndIsCorrectTrue(question.getId()).orElse(null);
                boolean isCorrect = selectedOption != null && correctOption != null
                        && selectedOption.getId().equals(correctOption.getId());

                answer.setSelectedOption(selectedOption);
                answer.setIsCorrect(isCorrect);
                answer.setScore(isCorrect ? questionScore : 0d);
                answer.setCorrectAnswerSnapshot(correctOption != null ? correctOption.getContent() : null);

                if (isCorrect) {
                    correctCount++;
                    totalScore += questionScore;
                } else {
                    wrongCount++;
                }
            } else {
                EssayAnswer sample = essayAnswerRepository.findByQuestion_Id(question.getId()).orElse(null);
                answer.setSelectedOption(null);
                answer.setIsCorrect(null);
                answer.setScore(0d);
                answer.setCorrectAnswerSnapshot(sample != null ? sample.getSampleAnswer() : null);
            }

            answersToSave.add(answer);
        }

        answerRepository.saveAll(answersToSave);

        int tabSwitchCount = safeNonNegative(request.getTabSwitchCount());
        int violationScore = safeNonNegative(request.getViolationScore());

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setScore(totalScore);
        attempt.setCorrectCount(correctCount);
        attempt.setWrongCount(wrongCount);
        attempt.setTotalQuestions(examQuestions.size());
        attempt.setDurationTaken(calculateDurationSeconds(attempt.getStartedAt(), now));
        attempt.setSubmittedAt(now);
        attempt.setTabSwitchCount(tabSwitchCount);
        attempt.setViolationScore(violationScore);
        attempt.setFlagged(tabSwitchCount >= TAB_SWITCH_FLAG_THRESHOLD || violationScore >= VIOLATION_SCORE_FLAG_THRESHOLD);
        attemptRepository.save(attempt);

        return new ApiResponse<>(HttpStatus.OK.value(), "Nộp bài thành công!", toAttemptDetailResponse(attempt));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<AttemptDetailResponse> getMyAttemptById(Integer attemptId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem lượt làm bài!");
        }

        Attempt attempt = attemptRepository.findByIdAndUser_Id(attemptId, currentUser.getId()).orElse(null);
        if (attempt == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lượt làm bài!");
        }

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy chi tiết lượt làm bài thành công!", toAttemptDetailResponse(attempt));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<AttemptSummaryResponse>> getMyAttempts() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem lịch sử làm bài!");
        }

        List<AttemptSummaryResponse> data = attemptRepository.findByUser_IdOrderByStartedAtDesc(currentUser.getId()).stream()
                .map(this::toAttemptSummaryResponse)
                .toList();

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy lịch sử làm bài thành công!", data);
    }

    private String validateStartConstraints(User user, Exam exam) {
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(exam.getIsActive())) {
            return "Đề thi hiện chưa được mở!";
        }

        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return "Đề thi chưa đến thời gian bắt đầu!";
        }

        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return "Đề thi đã kết thúc!";
        }

        if (attemptRepository.existsByUser_IdAndExam_IdAndStatus(user.getId(), exam.getId(), AttemptStatus.DOING)) {
            return "Bạn đang có một lượt làm bài chưa nộp cho đề thi này!";
        }

        Integer maxAttempts = exam.getMaxAttempts();
        if (maxAttempts != null && maxAttempts > 0) {
            long usedAttempts = attemptRepository.countByUser_IdAndExam_Id(user.getId(), exam.getId());
            if (usedAttempts >= maxAttempts) {
                return "Bạn đã dùng hết số lượt làm bài cho đề thi này!";
            }
        }

        return null;
    }

    private String validateSubmissionPayload(List<AttemptSubmitAnswerRequest> answers,
                                             Map<Integer, ExamQuestion> examQuestionMap) {
        Set<Integer> seenQuestionIds = new HashSet<>();
        for (AttemptSubmitAnswerRequest answer : answers) {
            Integer questionId = answer.getQuestionId();
            if (questionId == null) {
                return "Có câu trả lời thiếu questionId!";
            }
            if (!seenQuestionIds.add(questionId)) {
                return "Danh sách câu trả lời bị trùng questionId!";
            }
            if (!examQuestionMap.containsKey(questionId)) {
                return "Question id=" + questionId + " không thuộc đề thi này!";
            }

            Question question = examQuestionMap.get(questionId).getQuestion();
            if (question == null) {
                return "Question id=" + questionId + " không hợp lệ!";
            }

            if (question.getType() == QuestionType.MCQ) {
                Integer selectedOptionId = answer.getSelectedOptionId();
                if (selectedOptionId != null
                        && questionOptionRepository.findByIdAndQuestion_Id(selectedOptionId, questionId).isEmpty()) {
                    return "selectedOptionId không thuộc question id=" + questionId;
                }
            } else if (answer.getSelectedOptionId() != null) {
                return "Câu hỏi ESSAY không được gửi selectedOptionId (question id=" + questionId + ")";
            }
        }
        return null;
    }

    private QuestionOption resolveSelectedOption(Integer questionId, Integer selectedOptionId) {
        if (selectedOptionId == null) {
            return null;
        }
        return questionOptionRepository.findByIdAndQuestion_Id(selectedOptionId, questionId).orElse(null);
    }

    private LocalDateTime calculateDeadline(Attempt attempt) {
        LocalDateTime startedAt = attempt.getStartedAt();
        if (startedAt == null) {
            return null;
        }

        LocalDateTime deadline = null;
        Integer durationMinutes = attempt.getExam() != null ? attempt.getExam().getDuration() : null;
        if (durationMinutes != null && durationMinutes > 0) {
            deadline = startedAt.plusMinutes(durationMinutes);
        }

        LocalDateTime examEndTime = attempt.getExam() != null ? attempt.getExam().getEndTime() : null;
        if (deadline == null) {
            return examEndTime;
        }
        if (examEndTime != null && examEndTime.isBefore(deadline)) {
            return examEndTime;
        }
        return deadline;
    }

    private int calculateDurationSeconds(LocalDateTime startedAt, LocalDateTime endTime) {
        if (startedAt == null || endTime == null || endTime.isBefore(startedAt)) {
            return 0;
        }
        return (int) Duration.between(startedAt, endTime).getSeconds();
    }

    private AttemptSummaryResponse toAttemptSummaryResponse(Attempt attempt) {
        Exam exam = attempt.getExam();
        Integer examId = exam != null ? exam.getId() : null;
        String examTitle = exam != null ? exam.getTitle() : null;

        return new AttemptSummaryResponse(
                attempt.getId(),
                examId,
                examTitle,
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getCorrectCount(),
                attempt.getWrongCount(),
                attempt.getTotalQuestions(),
                attempt.getDurationTaken(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getExpiredAt(),
                attempt.getTabSwitchCount(),
                attempt.getViolationScore(),
                attempt.getFlagged()
        );
    }

    private AttemptDetailResponse toAttemptDetailResponse(Attempt attempt) {
        AttemptSummaryResponse summary = toAttemptSummaryResponse(attempt);

        List<AttemptAnswerResponse> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attempt.getId()).stream()
                .map(answer -> new AttemptAnswerResponse(
                        answer.getQuestion() != null ? answer.getQuestion().getId() : null,
                        answer.getQuestion() != null ? answer.getQuestion().getContent() : null,
                        answer.getSelectedOption() != null ? answer.getSelectedOption().getId() : null,
                        answer.getEssayAnswer(),
                        answer.getIsCorrect(),
                        answer.getScore()
                ))
                .toList();

        return new AttemptDetailResponse(
                summary.getId(),
                summary.getExamId(),
                summary.getExamTitle(),
                summary.getStatus(),
                summary.getScore(),
                summary.getCorrectCount(),
                summary.getWrongCount(),
                summary.getTotalQuestions(),
                summary.getDurationTaken(),
                summary.getStartedAt(),
                summary.getSubmittedAt(),
                summary.getExpiredAt(),
                summary.getTabSwitchCount(),
                summary.getViolationScore(),
                summary.getFlagged(),
                answers
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

    private int safeNonNegative(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}



