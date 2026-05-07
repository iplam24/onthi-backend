package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.attempt.dto.AttemptAnswerResponse;
import com.onthi.v_edu.attempt.dto.AttemptDetailResponse;
import com.onthi.v_edu.attempt.dto.AttemptFilterRequest;
import com.onthi.v_edu.attempt.dto.AttemptStartRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitAnswerRequest;
import com.onthi.v_edu.attempt.dto.AttemptSubmitRequest;
import com.onthi.v_edu.attempt.dto.AttemptSummaryResponse;
import com.onthi.v_edu.attempt.dto.ViolationRecordRequest;
import com.onthi.v_edu.attempt.dto.ViolationType;
import com.onthi.v_edu.attempt.entity.Answer;
import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.attempt.repository.AnswerRepository;
import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.common.constant.AttemptStatus;
import com.onthi.v_edu.common.constant.QuestionType;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
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
import com.onthi.v_edu.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserService userService;

    public AttemptServiceImpl(AttemptRepository attemptRepository,
                              AnswerRepository answerRepository,
                              ExamRepository examRepository,
                              ExamQuestionRepository examQuestionRepository,
                              QuestionOptionRepository questionOptionRepository,
                              EssayAnswerRepository essayAnswerRepository,
                              UserRepository userRepository,
                              UserService userService) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.essayAnswerRepository = essayAnswerRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public ApiResponse<AttemptDetailResponse> startAttempt(AttemptStartRequest request) {
        System.out.println("\n--- [DEBUG] AttemptService.startAttempt ---");
        System.out.println("Request Body: { examId: " + request.getExamId() + " }");

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            System.out.println("[DEBUG] Lỗi: Người dùng chưa đăng nhập.");
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để bắt đầu làm bài!");
        }
        System.out.println("User: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

            Exam exam = examRepository.findByIdAndDeletedAtIsNull(request.getExamId()).orElse(null);
        if (exam == null) {
            System.out.println("[DEBUG] Lỗi: Không tìm thấy đề thi với ID: " + request.getExamId());
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy đề thi!");
        }
        System.out.println("Exam: '" + exam.getTitle() + "' (ID: " + exam.getId() + ")");

        String antiCheatError = validateStartConstraints(currentUser, exam);
        if (antiCheatError != null) {
            System.out.println("[DEBUG] Lỗi validateStartConstraints: " + antiCheatError);
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), antiCheatError);
        }
        System.out.println("ValidateStartConstraints: OK");

            List<ExamQuestion> examQuestions = examQuestionRepository.findByExam_IdAndDeletedAtIsNullOrderByOrderIndexAscQuestion_IdAsc(exam.getId());
        if (examQuestions.isEmpty()) {
            System.out.println("[DEBUG] Lỗi: Đề thi không có câu hỏi.");
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Đề thi chưa có câu hỏi, không thể bắt đầu!");
        }
        System.out.println("Số lượng câu hỏi: " + examQuestions.size());

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
        System.out.println("Tạo thành công lượt làm bài (Attempt) với ID: " + attempt.getId());
        System.out.println("--- [DEBUG] Kết thúc startAttempt ---\n");

        return new ApiResponse<>(HttpStatus.CREATED.value(), "Bắt đầu làm bài thành công!", toAttemptDetailResponse(attempt));
    }

    @Override
    public ApiResponse<AttemptDetailResponse> submitAttempt(Integer attemptId, AttemptSubmitRequest request) {
        System.out.println("\n--- [DEBUG] AttemptService.submitAttempt ---");
        System.out.println("Attempt ID: " + attemptId);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            System.out.println("[DEBUG] Lỗi: Người dùng chưa đăng nhập.");
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để nộp bài!");
        }
        System.out.println("User: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");

        Attempt attempt = attemptRepository.findByIdAndUser_Id(attemptId, currentUser.getId()).orElse(null);
        if (attempt == null) {
            System.out.println("[DEBUG] Lỗi: Không tìm thấy lượt làm bài với ID: " + attemptId + " cho user này.");
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lượt làm bài!");
        }
        System.out.println("Tìm thấy lượt làm bài. Trạng thái hiện tại: " + attempt.getStatus());

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            System.out.println("[DEBUG] Lỗi: Lượt làm bài đã nộp trước đó.");
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Lượt làm bài này đã được nộp trước đó!");
        }
        if (attempt.getStatus() == AttemptStatus.EXPIRED) {
            System.out.println("[DEBUG] Lỗi: Lượt làm bài đã hết hạn.");
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Lượt làm bài này đã hết hạn!");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = calculateDeadline(attempt);
        if (deadline != null && !now.isBefore(deadline)) {
            System.out.println("[DEBUG] Lỗi: Đã hết thời gian làm bài.");
            expireAttempt(attempt, now);
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Đã hết thời gian làm bài, không thể nộp!");
        }

            List<ExamQuestion> examQuestions = examQuestionRepository.findByExam_IdAndDeletedAtIsNullOrderByOrderIndexAscQuestion_IdAsc(attempt.getExam().getId());
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
        System.out.println("Số câu trả lời nhận được: " + submittedAnswers.size());

        String antiCheatError = validateSubmissionPayload(submittedAnswers, examQuestionMap);
        if (antiCheatError != null) {
            System.out.println("[DEBUG] Lỗi validateSubmissionPayload: " + antiCheatError);
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), antiCheatError);
        }

        answerRepository.deleteByAttempt_Id(attempt.getId());

        double totalScore = 0d;
        int correctCount = 0;
        int wrongCount = 0;
        List<Answer> answersToSave = new ArrayList<>();

        System.out.println("\n  [DEBUG] Bắt đầu chấm điểm...");
        for (AttemptSubmitAnswerRequest submitted : submittedAnswers) {
            System.out.println("  ------------------------------------");
            System.out.println("  Chấm câu hỏi ID: " + submitted.getQuestionId());

            ExamQuestion examQuestion = examQuestionMap.get(submitted.getQuestionId());
            if (examQuestion == null || examQuestion.getQuestion() == null) {
                System.out.println("  [DEBUG] -> Bỏ qua vì không tìm thấy câu hỏi trong đề thi.");
                continue;
            }

            Question question = examQuestion.getQuestion();
            double questionScore = examQuestion.getScore() == null ? 1d : examQuestion.getScore();
            System.out.println("  Điểm của câu hỏi: " + questionScore);

            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setEssayAnswer(normalize(submitted.getEssayAnswer()));
            answer.setQuestionSnapshot(question.getContent());
            answer.setQuestionFormatSnapshot(question.getContentFormat());
            answer.setCreatedAt(now);
            answer.setUpdatedAt(now);

            if (question.getType() == QuestionType.MCQ) {
                System.out.println("  Loại câu hỏi: MCQ");
                Integer selectedOptionId = submitted.getSelectedOptionId();
                System.out.println("  Người dùng chọn option ID: " + selectedOptionId);

                QuestionOption selectedOption = resolveSelectedOption(question.getId(), selectedOptionId);
                        QuestionOption correctOption = questionOptionRepository.findFirstByQuestion_IdAndIsCorrectTrueAndDeletedAtIsNull(question.getId()).orElse(null);
                
                if (correctOption == null) {
                    System.out.println("  [DEBUG] -> Lỗi DB: Không tìm thấy đáp án đúng cho câu hỏi này trong database!");
                } else {
                    System.out.println("  Đáp án đúng là option ID: " + correctOption.getId());
                }

                boolean isCorrect = selectedOption != null && correctOption != null
                        && selectedOption.getId().equals(correctOption.getId());
                System.out.println("  => Kết quả: " + (isCorrect ? "ĐÚNG" : "SAI"));

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
                System.out.println("  Loại câu hỏi: ESSAY (Tự luận)");
                System.out.println("  => Kết quả: Cần chấm thủ công, điểm tạm tính là 0.");
                        EssayAnswer sample = essayAnswerRepository.findByQuestion_IdAndDeletedAtIsNull(question.getId()).orElse(null);
                answer.setSelectedOption(null);
                answer.setIsCorrect(null);
                answer.setScore(0d);
                answer.setCorrectAnswerSnapshot(sample != null ? sample.getSampleAnswer() : null);
            }

            answersToSave.add(answer);
        }
        System.out.println("  ------------------------------------");
        System.out.println("  [DEBUG] Chấm điểm hoàn tất.");

        answerRepository.saveAll(answersToSave);
        System.out.println("Lưu " + answersToSave.size() + " câu trả lời vào database.");

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
        userService.recordStudyActivity(currentUser.getId(), now.toLocalDate());

        System.out.println("\nKết quả cuối cùng:");
        System.out.println("  - Tổng điểm: " + totalScore);
        System.out.println("  - Số câu đúng: " + correctCount);
        System.out.println("  - Số câu sai: " + wrongCount);
        System.out.println("Lưu kết quả vào lượt làm bài ID: " + attempt.getId());
        System.out.println("--- [DEBUG] Kết thúc submitAttempt ---\n");

        return new ApiResponse<>(HttpStatus.OK.value(), "Nộp bài thành công!", toAttemptDetailResponse(attempt));
    }

    @Override
    public void expireOverdueAttempts() {
        LocalDateTime now = LocalDateTime.now();
        List<Attempt> doingAttempts = attemptRepository.findByStatus(AttemptStatus.DOING);

        for (Attempt attempt : doingAttempts) {
            if (isDeadlineReached(attempt, now)) {
                System.out.println("[DEBUG] Auto-expiring attempt ID: " + attempt.getId());
                expireAttempt(attempt, now);
            }
        }
    }

    @Override
    public ApiResponse<AttemptDetailResponse> recordViolation(Integer attemptId, ViolationRecordRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập!");
        }

        Attempt attempt = attemptRepository.findByIdAndUser_Id(attemptId, currentUser.getId()).orElse(null);
        if (attempt == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lượt làm bài!");
        }

        if (attempt.getStatus() != AttemptStatus.DOING) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Không thể ghi nhận vi phạm cho lượt làm bài đã kết thúc!");
        }

        if (request.getType() == ViolationType.TAB_SWITCH) {
            attempt.setTabSwitchCount(attempt.getTabSwitchCount() + 1);
        }

        if (attempt.getTabSwitchCount() >= TAB_SWITCH_FLAG_THRESHOLD) {
            attempt.setFlagged(true);
        }

        attempt = attemptRepository.save(attempt);
        return new ApiResponse<>(HttpStatus.OK.value(), "Ghi nhận vi phạm thành công!", toAttemptDetailResponse(attempt));
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
    public ApiResponse<PageResponse<AttemptSummaryResponse>> getMyAttempts(AttemptFilterRequest filter, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem lịch sử làm bài!");
        }

        AttemptFilterRequest effectiveFilter = filter == null ? new AttemptFilterRequest() : filter;
        String keyword = normalize(effectiveFilter.getKeyword());
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }

        Page<AttemptSummaryResponse> data = attemptRepository.searchMyAttempts(
                        currentUser.getId(),
                        effectiveFilter.getSubjectId(),
                        effectiveFilter.getLevelId(),
                        effectiveFilter.getExamId(),
                        effectiveFilter.getStatus(),
                        effectiveFilter.getFlagged(),
                        effectiveFilter.getFrom(),
                        effectiveFilter.getTo(),
                        keyword,
                        pageable)
                .map(this::toAttemptSummaryResponse);

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy lịch sử làm bài thành công!", PageResponse.from(data));
    }

    private String validateStartConstraints(User user, Exam exam) {
        System.out.println("  [DEBUG] Bắt đầu validateStartConstraints...");
        LocalDateTime now = LocalDateTime.now();
        System.out.println("  [DEBUG] Thời gian hiện tại: " + now);

        if (!Boolean.TRUE.equals(exam.getIsActive())) {
            System.out.println("  [DEBUG] -> Lỗi: Đề thi không active. isActive = " + exam.getIsActive());
            return "Đề thi hiện chưa được mở!";
        }
        System.out.println("  [DEBUG] -> Check isActive: OK");

        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            System.out.println("  [DEBUG] -> Lỗi: Chưa đến thời gian làm bài. StartTime = " + exam.getStartTime());
            return "Đề thi chưa đến thời gian bắt đầu!";
        }
        System.out.println("  [DEBUG] -> Check StartTime: OK");

        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            System.out.println("  [DEBUG] -> Lỗi: Đã quá thời gian làm bài. EndTime = " + exam.getEndTime());
            return "Đề thi đã kết thúc!";
        }
        System.out.println("  [DEBUG] -> Check EndTime: OK");

        boolean hasDoingAttempt = attemptRepository.existsByUser_IdAndExam_IdAndStatus(user.getId(), exam.getId(), AttemptStatus.DOING);
        if (hasDoingAttempt) {
            System.out.println("  [DEBUG] -> Lỗi: Tìm thấy một lượt làm bài đang 'DOING'.");
            return "Bạn đang có một lượt làm bài chưa nộp cho đề thi này!";
        }
        System.out.println("  [DEBUG] -> Check Doing Attempt: OK");

        Integer maxAttempts = exam.getMaxAttempts();
        if (maxAttempts != null && maxAttempts > 0) {
            long usedAttempts = attemptRepository.countByUser_IdAndExam_Id(user.getId(), exam.getId());
            System.out.println("  [DEBUG] -> Check MaxAttempts: max=" + maxAttempts + ", used=" + usedAttempts);
            if (usedAttempts >= maxAttempts) {
                System.out.println("  [DEBUG] -> Lỗi: Đã hết số lần làm bài.");
                return "Bạn đã dùng hết số lượt làm bài cho đề thi này!";
            }
            System.out.println("  [DEBUG] -> Check MaxAttempts: OK");
        } else {
            System.out.println("  [DEBUG] -> Check MaxAttempts: Không giới hạn số lần làm.");
        }

        System.out.println("  [DEBUG] Kết thúc validateStartConstraints: Tất cả đều hợp lệ.");
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
                                        && questionOptionRepository.findByIdAndQuestion_IdAndDeletedAtIsNull(selectedOptionId, questionId).isEmpty()) {
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
            return questionOptionRepository.findByIdAndQuestion_IdAndDeletedAtIsNull(selectedOptionId, questionId).orElse(null);
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

    private boolean isDeadlineReached(Attempt attempt, LocalDateTime now) {
        LocalDateTime deadline = calculateDeadline(attempt);
        return deadline != null && !now.isBefore(deadline);
    }

    private void expireAttempt(Attempt attempt, LocalDateTime now) {
        if (attempt == null || attempt.getStatus() != AttemptStatus.DOING) {
            return;
        }

        attempt.setStatus(AttemptStatus.EXPIRED);
        attempt.setExpiredAt(now);
        attempt.setDurationTaken(calculateDurationSeconds(attempt.getStartedAt(), now));
        attemptRepository.save(attempt);
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
        Integer subjectId = getSubjectId(exam);
        String subjectName = getSubjectName(exam);
        Integer subjectLevelId = getSubjectLevelId(exam);
        String subjectLevelName = getSubjectLevelName(exam);

        return new AttemptSummaryResponse(
                attempt.getId(),
                examId,
                examTitle,
                subjectId,
                subjectName,
                subjectLevelId,
                subjectLevelName,
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

        List<AttemptAnswerResponse> answers = answerRepository.findByAttemptIdWithDetails(attempt.getId()).stream()
                .map(answer -> new AttemptAnswerResponse(
                        answer.getQuestion() != null ? answer.getQuestion().getId() : null,
                        answer.getQuestion() != null ? answer.getQuestion().getContent() : null,
                        answer.getQuestionFormatSnapshot(),
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
                summary.getSubjectId(),
                summary.getSubjectName(),
                summary.getSubjectLevelId(),
                summary.getSubjectLevelName(),
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

    private Integer getSubjectId(Exam exam) {
        return exam != null && exam.getSubject() != null ? exam.getSubject().getId() : null;
    }

    private String getSubjectName(Exam exam) {
        return exam != null && exam.getSubject() != null ? exam.getSubject().getName() : null;
    }

    private Integer getSubjectLevelId(Exam exam) {
        return exam != null && exam.getSubject() != null && exam.getSubject().getLevel() != null
                ? exam.getSubject().getLevel().getId()
                : null;
    }

    private String getSubjectLevelName(Exam exam) {
        return exam != null && exam.getSubject() != null && exam.getSubject().getLevel() != null
                ? exam.getSubject().getLevel().getName()
                : null;
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
