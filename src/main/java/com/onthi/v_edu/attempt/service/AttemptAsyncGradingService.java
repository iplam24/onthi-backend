package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.attempt.entity.Answer;
import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.attempt.repository.AnswerRepository;
import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.common.constant.AttemptStatus;
import com.onthi.v_edu.common.constant.QuestionType;
import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.repository.ExamQuestionRepository;
import com.onthi.v_edu.question.entity.EssayAnswer;
import com.onthi.v_edu.question.entity.QuestionOption;
import com.onthi.v_edu.question.repository.EssayAnswerRepository;
import com.onthi.v_edu.question.repository.QuestionOptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttemptAsyncGradingService {

    private static final Logger logger = LoggerFactory.getLogger(AttemptAsyncGradingService.class);

    private final AttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final EssayAnswerRepository essayAnswerRepository;
    private final GeminiAiGradingService geminiAiGradingService;
    private final GitHubModelsAiGradingService gitHubModelsAiGradingService;

    public AttemptAsyncGradingService(AttemptRepository attemptRepository,
                                     AnswerRepository answerRepository,
                                     ExamQuestionRepository examQuestionRepository,
                                     QuestionOptionRepository questionOptionRepository,
                                     EssayAnswerRepository essayAnswerRepository,
                                     GeminiAiGradingService geminiAiGradingService,
                                     GitHubModelsAiGradingService gitHubModelsAiGradingService) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.essayAnswerRepository = essayAnswerRepository;
        this.geminiAiGradingService = geminiAiGradingService;
        this.gitHubModelsAiGradingService = gitHubModelsAiGradingService;
    }

    @Transactional
    public void gradeAttemptAsync(Integer attemptId) {
        try {
            logger.info("[ASYNC GRADING] Bắt đầu chấm bài cho attempt ID: {}", attemptId);

            Optional<Attempt> attemptOpt = attemptRepository.findById(attemptId);
            if (attemptOpt.isEmpty()) {
                logger.error("[ASYNC GRADING] Không tìm thấy attempt ID: {}", attemptId);
                return;
            }

            Attempt attempt = attemptOpt.get();
            if (attempt.getStatus() != AttemptStatus.GRADING) {
                logger.info("[ASYNC GRADING] Attempt ID {} không ở trạng thái GRADING, bỏ qua.", attemptId);
                return;
            }

            List<Answer> allAnswers = answerRepository.findByAttempt_IdOrderByIdAsc(attemptId);
            List<ExamQuestion> examQuestions = examQuestionRepository.findByExam_IdAndDeletedAtIsNullOrderByOrderIndexAscQuestion_IdAsc(attempt.getExam().getId());
            
            // Map để lấy điểm tối đa của từng câu hỏi
            Map<Integer, Double> questionMaxScores = examQuestions.stream()
                    .filter(eq -> eq.getQuestion() != null)
                    .collect(Collectors.toMap(eq -> eq.getQuestion().getId(), eq -> eq.getScore() != null ? eq.getScore() : 1.0, (existing, replacement) -> existing));

            // Lấy ID của tất cả các câu hỏi để fetch data 1 lần
            List<Integer> questionIds = allAnswers.stream().map(a -> a.getQuestion().getId()).collect(Collectors.toList());
            
            // Fetch tất cả Options và EssayAnswers liên quan để tránh N+1
            List<QuestionOption> allOptions = questionOptionRepository.findByQuestion_IdInAndDeletedAtIsNull(questionIds);
            Map<Integer, List<QuestionOption>> optionsByQuestionId = allOptions.stream().collect(Collectors.groupingBy(qo -> qo.getQuestion().getId()));
            
            List<EssayAnswer> allEssayAnswers = essayAnswerRepository.findByQuestion_IdInAndDeletedAtIsNull(questionIds);
            Map<Integer, EssayAnswer> essayAnswerByQuestionId = allEssayAnswers.stream().collect(Collectors.toMap(ea -> ea.getQuestion().getId(), ea -> ea));

            List<GitHubModelsAiGradingService.BatchItem> batchItems = new ArrayList<>();

            for (Answer answer : allAnswers) {
                Integer qId = answer.getQuestion().getId();
                double maxScore = questionMaxScores.getOrDefault(qId, 1.0);
                QuestionType qType = answer.getQuestion().getType();
                
                List<QuestionOption> qOptions = optionsByQuestionId.getOrDefault(qId, new ArrayList<>());
                List<String> optionTexts = qOptions.stream().map(QuestionOption::getContent).collect(Collectors.toList());
                
                String studentAnswerText = "";
                String correctAnswerText = "";

                if (qType == QuestionType.MCQ) {
                    // 1. CHẤM ĐIỂM TRẮC NGHIỆM (LOCAL) - KHÔNG GỬI QUA AI
                    QuestionOption correctOption = qOptions.stream().filter(QuestionOption::getIsCorrect).findFirst().orElse(null);
                    correctAnswerText = correctOption != null ? correctOption.getContent() : "(Chưa có đáp án đúng)";
                    answer.setCorrectAnswerSnapshot(correctAnswerText);
                    
                    studentAnswerText = answer.getSelectedOption() != null ? 
                            qOptions.stream().filter(o -> o.getId().equals(answer.getSelectedOption().getId())).findFirst()
                                    .map(QuestionOption::getContent).orElse("(Không chọn)") : "(Không chọn)";
                    
                    boolean isCorrect = answer.getSelectedOption() != null && correctOption != null && 
                                       answer.getSelectedOption().getId().equals(correctOption.getId());
                    
                    answer.setIsCorrect(isCorrect);
                    answer.setScore(isCorrect ? maxScore : 0.0);
                    answer.setAiFeedback(isCorrect ? "Đáp án chính xác." : "Đáp án sai. Đáp án đúng là: " + correctAnswerText);
                    answer.setAiGradingMethod("Nhận xét của giáo viên");
                    
                    logger.info("[ASYNC GRADING] Câu hỏi MCQ (ID: {}) chấm LOCAL, không gửi AI.", qId);
                    continue; 
                } else {
                    // 2. CHẤM ĐIỂM TỰ LUẬN
                    EssayAnswer sample = essayAnswerByQuestionId.get(qId);
                    correctAnswerText = sample != null ? sample.getSampleAnswer() : "(Không có đáp án mẫu)";
                    answer.setCorrectAnswerSnapshot(correctAnswerText);
                    studentAnswerText = answer.getEssayAnswer() != null ? answer.getEssayAnswer() : "(Trống)";

                    // Kiểm tra độ dài: Nếu dưới 30 ký tự thì so sánh trực tiếp, không gửi AI
                    if (studentAnswerText.trim().length() < 30) {
                        boolean match = studentAnswerText.trim().equalsIgnoreCase(correctAnswerText.trim());
                        answer.setScore(match ? maxScore : 0.0);
                        answer.setIsCorrect(match);
                        answer.setAiFeedback(match ? "Đáp án ngắn chính xác." : "Đáp án quá ngắn hoặc không khớp với đáp án mẫu.");
                        answer.setAiGradingMethod("Nhận xét của giáo viên");
                        
                        logger.info("[ASYNC GRADING] Câu hỏi Essay ngắn (ID: {}, len: {}) chấm LOCAL, không gửi AI.", qId, studentAnswerText.trim().length());
                        continue;
                    }
                }

                // Nếu là Tự luận dài (>30 ký tự) mới đưa vào danh sách gửi AI GitHub Models
                batchItems.add(new GitHubModelsAiGradingService.BatchItem(
                        qId,
                        answer.getQuestionSnapshot(),
                        studentAnswerText,
                        correctAnswerText,
                        maxScore,
                        qType.name(),
                        optionTexts
                ));
            }

            if (!batchItems.isEmpty()) {
                logger.info("[ASYNC GRADING] Gửi {} câu hỏi tới GitHub Models để chấm bài", batchItems.size());
                
                Map<Integer, GitHubModelsAiGradingService.AiGradingResult> aiResults = 
                        gitHubModelsAiGradingService.gradeBatchWithGitHubModels(attempt.getExam().getTitle(), batchItems);

                for (Answer answer : allAnswers) {
                    GitHubModelsAiGradingService.AiGradingResult result = aiResults.get(answer.getQuestion().getId());
                    if (result != null) {
                        answer.setScore(result.getScore());
                        answer.setIsCorrect(result.getIsCorrect());
                        answer.setAiFeedback(result.getFeedback());
                        answer.setAiGradingMethod("Nhận xét của giáo viên");
                    } else if (answer.getAiGradingMethod() == null) {
                        // Chỉ handle failure nếu chưa được chấm Local trước đó
                        handleGradingFailure(answer);
                    }
                }
                answerRepository.saveAll(allAnswers);
            }

            double totalScore = 0;
            int correctCount = 0;
            int wrongCount = 0;

            for (Answer answer : allAnswers) {
                double s = answer.getScore() != null ? answer.getScore() : 0.0;
                totalScore += s;
                if (answer.getIsCorrect() != null && answer.getIsCorrect()) {
                    correctCount++;
                } else {
                    wrongCount++;
                }
            }

            attempt.setScore(totalScore);
            attempt.setCorrectCount(correctCount);
            attempt.setWrongCount(wrongCount);
            attempt.setStatus(AttemptStatus.SUBMITTED);
            attemptRepository.save(attempt);

            logger.info("[ASYNC GRADING] Hoàn tất chấm bài cho attempt ID: {}. Tổng điểm: {}, Số câu đúng: {}", 
                    attemptId, totalScore, correctCount);
                    
        } catch (Exception e) {
            logger.error("[ASYNC GRADING] Lỗi nghiêm trọng khi chấm bài ID {}: {}", attemptId, e.getMessage(), e);
        }
    }

    private void handleGradingFailure(Answer answer) {
        if (answer.getQuestion().getType() == QuestionType.MCQ) {
            // MCQ có thể tự check nếu AI fail (Logic fallback)
            // Giữ nguyên kết quả từ AttemptServiceImpl nếu cần, hoặc log lỗi
            logger.warn("[ASYNC GRADING] AI không trả về kết quả cho MCQ ID: {}", answer.getQuestion().getId());
        } else {
            answer.setScore(0.0);
            answer.setIsCorrect(false);
            answer.setAiFeedback("AI không cung cấp phản hồi cho câu hỏi này.");
            answer.setAiGradingMethod("FAILED");
        }
    }
}
