package com.onthi.v_edu.ai.service;

import com.onthi.v_edu.common.ai.GitHubModelsClientService;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.entity.QuestionOption;
import com.onthi.v_edu.question.repository.QuestionOptionRepository;
import com.onthi.v_edu.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiTutorService {

    private static final Logger logger = LoggerFactory.getLogger(AiTutorService.class);
    private final GitHubModelsClientService aiClientService;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    public String explainQuestion(Integer questionId, String studentAnswer) {
        Question question = questionRepository.findById(questionId).orElse(null);
        if (question == null) return "Không tìm thấy câu hỏi.";

        List<QuestionOption> options = questionOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByIdAsc(questionId);
        String optionsText = options.stream()
                .map(o -> "- " + o.getContent() + (o.getIsCorrect() ? " (Đúng)" : ""))
                .collect(Collectors.joining("\n"));

        String groupContext = "";
        if (question.getQuestionGroup() != null) {
            groupContext = "ĐOẠN VĂN (NGỮ LIỆU CHUNG):\n" + question.getQuestionGroup().getContent() + "\n\n";
        }

        String prompt = String.format("""
                Bạn là một gia sư AI tận tâm. Hãy giải thích câu hỏi sau đây cho học sinh.
                
                %sCÂU HỎI:
                %s
                
                CÁC LỰA CHỌN (nếu có):
                %s
                
                %s
                
                NHIỆM VỤ:
                1. Giải thích tại sao đáp án đúng lại là đáp án đó.
                2. Nếu có câu trả lời của học sinh, hãy phân tích tại sao họ đúng hoặc sai.
                3. Cung cấp thêm một chút kiến thức liên quan để học sinh ghi nhớ lâu hơn.
                
                Hãy trả lời bằng Tiếng Việt, giọng văn khích lệ và dễ hiểu.
                """,
                groupContext,
                question.getContent(),
                optionsText.isEmpty() ? "Câu hỏi tự luận" : optionsText,
                studentAnswer != null && !studentAnswer.isEmpty() ? "CÂU TRẢ LỜI CỦA HỌC SINH: " + studentAnswer : ""
        );

        logger.info("[AI TUTOR] Explaining question ID using GitHub Models: {}", questionId);
        return aiClientService.generateContent(prompt, "Bạn là gia sư AI chuyên nghiệp.");
    }
}
