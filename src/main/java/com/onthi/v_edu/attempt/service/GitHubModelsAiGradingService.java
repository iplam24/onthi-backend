package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.common.ai.GitHubModelsClientService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fallback chấm bài bằng GitHub Models (OpenAI-compatible API).
 */
@Service
public class GitHubModelsAiGradingService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubModelsAiGradingService.class);

    private final GitHubModelsClientService aiClientService;

    public GitHubModelsAiGradingService(GitHubModelsClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

    public static class BatchItem {
        private final Integer questionId;
        private final String questionText;
        private final String studentAnswer;
        private final String sampleAnswer;
        private final double maxScore;
        private final String questionType;
        private final List<String> options;

        public BatchItem(Integer questionId, String questionText, String studentAnswer, String sampleAnswer,
                double maxScore, String questionType, List<String> options) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.studentAnswer = studentAnswer;
            this.sampleAnswer = sampleAnswer;
            this.maxScore = maxScore;
            this.questionType = questionType;
            this.options = options;
        }

        public Integer getQuestionId() {
            return questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getStudentAnswer() {
            return studentAnswer;
        }

        public String getSampleAnswer() {
            return sampleAnswer;
        }

        public double getMaxScore() {
            return maxScore;
        }

        public String getQuestionType() {
            return questionType;
        }

        public List<String> getOptions() {
            return options;
        }
    }

    public AiGradingResult gradeWithGitHubModels(String questionText, String studentAnswer, String sampleAnswer,
            double maxScore) {
        if (!aiClientService.isEnabled()) {
            return new AiGradingResult(null, false, "GitHub Models AI bị tắt", null);
        }

        try {
            String prompt = buildPrompt(questionText, studentAnswer, sampleAnswer, maxScore);
            logger.info("[GITHUB MODELS] Sending grading request using aiClientService");
            
            String responseText = aiClientService.generateContent(prompt, "Bạn là giáo viên chấm bài tự luận. Chỉ trả JSON hợp lệ.");
            
            if (responseText == null) {
                return new AiGradingResult(null, false, "GitHub Models trả về kết quả rỗng", null);
            }

            logger.info("[GITHUB MODELS] Response text received.");
            return parseResponse(responseText, maxScore);
        } catch (Exception e) {
            logger.error("[GITHUB MODELS] Unexpected error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Lỗi gọi GitHub Models", null);
        }
    }

    public Map<Integer, AiGradingResult> gradeBatchWithGitHubModels(String examTitle, List<BatchItem> items) {
        if (!aiClientService.isEnabled() || items == null || items.isEmpty()) {
            return new HashMap<>();
        }

        Map<Integer, AiGradingResult> finalResults = new HashMap<>();

        // Chia chunk dựa trên tổng độ dài tích lũy để đảm bảo không vượt quá token
        // limit
        List<List<BatchItem>> chunks = new ArrayList<>();
        List<BatchItem> currentChunk = new ArrayList<>();
        long currentChunkLength = 0;

        for (BatchItem item : items) {
            long itemLength = (item.getStudentAnswer() != null ? item.getStudentAnswer().length() : 0) +
                    (item.getSampleAnswer() != null ? item.getSampleAnswer().length() : 0);

            // Nếu thêm item này vào mà vượt quá 8000 ký tự thì đóng chunk cũ
            if (!currentChunk.isEmpty() && (currentChunkLength + itemLength > 8000)) {
                chunks.add(new ArrayList<>(currentChunk));
                currentChunk.clear();
                currentChunkLength = 0;
            }

            currentChunk.add(item);
            currentChunkLength += itemLength;
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        logger.info("[GITHUB MODELS BATCH] Tổng số câu: {}. Đã chia thành {} chunks dựa trên độ dài.", items.size(),
                chunks.size());

        int chunkCount = 0;
        for (List<BatchItem> chunk : chunks) {
            chunkCount++;
            // Truncate nội dung cực dài (phòng hờ trường hợp 1 câu đơn lẻ đã quá giới hạn)
            List<BatchItem> truncatedChunk = chunk.stream().map(it -> new BatchItem(
                    it.getQuestionId(),
                    it.getQuestionText(),
                    truncate(it.getStudentAnswer(), 5000),
                    truncate(it.getSampleAnswer(), 5000),
                    it.getMaxScore(),
                    it.getQuestionType(),
                    it.getOptions())).collect(Collectors.toList());

            try {
                String prompt = buildBatchGradingPrompt(examTitle, truncatedChunk);

                logger.info("[GITHUB MODELS BATCH] Sending chunk {}/{} ({} items)", chunkCount, chunks.size(),
                        truncatedChunk.size());
                
                String responseText = aiClientService.generateContent(prompt, "Bạn là giáo viên chấm bài tự luận. Chỉ trả JSON hợp lệ.");
                
                if (responseText != null) {
                    Map<Integer, AiGradingResult> chunkResults = parseBatchGitHubModelsResponse(responseText,
                            truncatedChunk);
                    finalResults.putAll(chunkResults);
                }
            } catch (Exception e) {
                logger.error("[GITHUB MODELS BATCH] Error in chunk {}: {}", chunkCount, e.getMessage());
            }
        }

        return finalResults;
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max)
            return text;
        return text.substring(0, max) + "... (nội dung bị cắt bớt do quá dài)";
    }

    private AiGradingResult parseResponse(String responseText, double maxScore) {
        if (responseText == null || responseText.isBlank()) {
            return new AiGradingResult(null, false, "GitHub Models không trả nội dung", null);
        }

        try {
            String cleaned = responseText.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            Double score = extractDouble(cleaned, "score");
            Boolean isCorrect = extractBoolean(cleaned, "isCorrect");
            String feedback = extractString(cleaned, "feedback");

            if (score == null) {
                return new AiGradingResult(null, false, "Không parse được score từ GitHub Models", feedback);
            }
            if (score > maxScore) {
                score = maxScore;
            }

            return new AiGradingResult(score, isCorrect != null ? isCorrect : score >= maxScore * 0.8,
                    "Nhận xét của giáo viên", feedback);
        } catch (Exception e) {
            logger.warn("[GITHUB MODELS] Parse error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Lỗi parse GitHub Models", null);
        }
    }

    private String buildPrompt(String questionText,
            String studentAnswer,
            String sampleAnswer,
            double maxScore) {

        return String.format("""
                Bạn là hệ thống AI chấm bài học thuật chuyên nghiệp.

                MỤC TIÊU:
                - Chấm điểm chính xác, công bằng, nghiêm túc.
                - Đánh giá dựa trên mức độ đúng kiến thức.
                - KHÔNG chấm theo cảm tính.

                =========================
                QUY TẮC CHẤM QUAN TRỌNG
                =========================
                
                1. BỎ QUA TIỀN TỐ/NHÃN (LABEL):
                   - Hãy bỏ qua các phần như "Thể thơ:", "Đáp án:", "Câu X:", "Trả lời:"... ở cả bài làm và đáp án mẫu. 
                   - Ví dụ: Học sinh ghi "Tự do" và đáp án là "Thể thơ: Tự do" => Coi như KHỚP HOÀN TOÀN.

                2. Với môn VĂN / NGÔN NGỮ:
                   - Đánh giá đúng nội dung, luận điểm và dẫn chứng.
                   - Không yêu cầu giống hoàn toàn đáp án mẫu về mặt câu chữ, chỉ cần đúng ý.

                3. PHẢN HỒI TRỰC QUAN:
                   - Trong phần feedback, hãy thêm mục **So sánh:** để học sinh dễ đối chiếu.
                   - Sử dụng format Markdown (bullet points, code blocks `...`) để làm nổi bật.

                =========================
                ĐỊNH DẠNG PHẢN HỒI (JSON)
                =========================

                {
                  "score": number,
                  "isCorrect": boolean,
                  "feedback": "string"
                }

                Trong đó 'feedback' phải bao gồm:
                1. Nhận xét chung.
                2. **So sánh:** (Đối chiếu giữa bài làm và đáp án mẫu).
                3. Gợi ý (nếu có).

                =========================
                DỮ LIỆU CẦN CHẤM
                =========================

                MAX_SCORE: %.2f
                QUESTION: %s
                SAMPLE_ANSWER: %s
                STUDENT_ANSWER: %s
                """,
                maxScore,
                questionText,
                sampleAnswer,
                studentAnswer);
    }

    private String buildBatchGradingPrompt(String examTitle, List<BatchItem> items) {
        StringBuilder questionsBlock = new StringBuilder();
        for (BatchItem item : items) {
            questionsBlock.append(String.format("""
                    ---
                    QUESTION_ID: %d
                    TYPE: %s
                    MAX_SCORE: %.2f
                    QUESTION_TITLE: %s
                    OPTIONS: %s
                    SAMPLE_ANSWER (CORRECT): %s
                    STUDENT_ANSWER: %s
                    """,
                    item.getQuestionId(),
                    item.getQuestionType(),
                    item.getMaxScore(),
                    item.getQuestionText(),
                    (item.getOptions() != null && !item.getOptions().isEmpty()) ? String.join(", ", item.getOptions())
                            : "N/A",
                    item.getSampleAnswer() != null ? item.getSampleAnswer() : "(N/A)",
                    item.getStudentAnswer()));
        }

        return String.format(
                """
                        Bạn là hệ thống AI chấm bài học thuật chuyên nghiệp đang chấm bài cho đề thi: "%s".

                        NHIỆM VỤ:
                        Hãy chấm điểm danh sách các câu hỏi dưới đây. Mỗi câu hỏi có yêu cầu (QUESTION_TITLE) và đáp án mẫu (SAMPLE_ANSWER) riêng biệt.

                        =========================
                        QUY TẮC CHẤM CHUYÊN SÂU
                        =========================

                        1. BỎ QUA TIỀN TỐ/NHÃN (LABEL):
                           - Hãy bỏ qua các phần như "Thể thơ:", "Đáp án:", "Câu X:", "Trả lời:"... ở cả bài làm và đáp án mẫu khi so khớp ý tưởng.

                        2. VỚI MÔN VĂN HỌC / XÃ HỘI:
                           - Đánh giá sự hiểu bài thông qua luận điểm và dẫn chứng.
                           - Phải chỉ rõ học sinh làm đúng ý nào, thiếu ý nào.

                        3. PHẢN HỒI TRỰC QUAN (feedback):
                           - Phải RIÊNG BIỆT cho từng câu.
                           - PHẢI có mục **So sánh:** để học sinh thấy sự tương quan giữa câu trả lời của mình và đáp án mẫu.
                           - Ví dụ:
                             **So sánh:**
                             - Bạn: `Tự do`
                             - Đáp án: `Tự do` (trong "Thể thơ: Tự do")
                             => Hoàn toàn chính xác.

                        YÊU CẦU ĐỊNH DẠNG:
                        - Trả về DUY NHẤT một mảng JSON (hoặc object có key "results").
                        - Mỗi đối tượng:
                        {
                          "questionId": number,
                          "score": number,
                          "isCorrect": boolean,
                          "feedback": "string (bao gồm Nhận xét, **So sánh**, Uu/Nhược điểm)"
                        }

                        DANH SÁCH CÂU HỎI CẦN CHẤM:
                        %s
                        """,
                examTitle, questionsBlock.toString());
    }

    private Map<Integer, AiGradingResult> parseBatchGitHubModelsResponse(String responseText, List<BatchItem> items) {
        Map<Integer, AiGradingResult> results = new HashMap<>();
        if (responseText == null || responseText.isEmpty())
            return results;

        try {
            String cleaned = responseText.replaceAll("```json", "").replaceAll("```", "").trim();

            List<Map<String, Object>> rawResults = null;

            // ObjectMapper có thể parse ra Map hoặc List tùy vào JSON đầu vào
            Object parsed = aiClientService.getObjectMapper().readValue(cleaned, Object.class);

            if (parsed instanceof List) {
                rawResults = (List<Map<String, Object>>) parsed;
            } else if (parsed instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) parsed;
                // Thử tìm trong các key phổ biến như "results", "answers", hoặc "items"
                if (map.containsKey("results") && map.get("results") instanceof List) {
                    rawResults = (List<Map<String, Object>>) map.get("results");
                } else if (map.containsKey("items") && map.get("items") instanceof List) {
                    rawResults = (List<Map<String, Object>>) map.get("items");
                } else {
                    // Nếu không có key bọc ngoài, có thể chính là một object đơn lẻ (nếu chỉ chấm 1
                    // câu)
                    // Hoặc ta coi toàn bộ map là một phần tử nếu nó có questionId
                    if (map.containsKey("questionId")) {
                        rawResults = List.of(map);
                    }
                }
            }

            if (rawResults == null) {
                logger.error("[GITHUB MODELS BATCH] Không tìm thấy mảng kết quả trong JSON: {}", cleaned);
                return results;
            }

            Map<Integer, BatchItem> itemMap = items.stream()
                    .collect(Collectors.toMap(BatchItem::getQuestionId, it -> it));

            for (Map<String, Object> raw : rawResults) {
                try {
                    Object qIdObj = raw.get("questionId");
                    if (qIdObj == null)
                        continue;

                    Integer qId = Integer.valueOf(qIdObj.toString());
                    Double score = Double.valueOf(raw.getOrDefault("score", 0.0).toString());

                    Object isCorrectObj = raw.get("isCorrect");
                    Boolean isCorrect = isCorrectObj instanceof Boolean ? (Boolean) isCorrectObj : null;

                    String feedback = (String) raw.getOrDefault("feedback", "");

                    BatchItem item = itemMap.get(qId);
                    double maxScore = item != null ? item.getMaxScore() : 1.0;
                    if (score > maxScore)
                        score = maxScore;

                    // Nếu isCorrect null, tính toán dựa trên score
                    if (isCorrect == null) {
                        isCorrect = score >= maxScore * 0.8;
                    }

                    results.put(qId, new AiGradingResult(score, isCorrect, "Nhận xét của giáo viên", feedback));
                } catch (Exception e) {
                    logger.warn("[GITHUB MODELS BATCH] Lỗi parse từng item: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("[GITHUB MODELS BATCH] Parse error: {}", e.getMessage());
        }
        return results;
    }

    private String shortMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank())
            return "Không có nội dung lỗi";
        String compact = rawBody.replace("\n", " ").replace("\r", " ").trim();
        return compact.length() > 180 ? compact.substring(0, 180) + "..." : compact;
    }

    private Double extractDouble(String json, String key) {
        String value = extractValue(json, key);
        if (value == null)
            return null;
        return Double.parseDouble(value.trim().replace(',', '.'));
    }

    private Boolean extractBoolean(String json, String key) {
        String value = extractValue(json, key);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    private String extractString(String json, String key) {
        return extractValue(json, key);
    }

    private String extractValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1)
            return null;
        int colon = json.indexOf(':', idx);
        if (colon == -1)
            return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start)))
            start++;
        if (start >= json.length())
            return null;

        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}')
            end++;
        return json.substring(start, end).trim();
    }

    public static class AiGradingResult {
        private final Double score;
        private final Boolean isCorrect;
        private final String gradingMethod;
        private final String feedback;

        public AiGradingResult(Double score, Boolean isCorrect, String gradingMethod, String feedback) {
            this.score = score;
            this.isCorrect = isCorrect;
            this.gradingMethod = gradingMethod;
            this.feedback = feedback;
        }

        public Double getScore() {
            return score;
        }

        public Boolean getIsCorrect() {
            return isCorrect;
        }

        public String getGradingMethod() {
            return gradingMethod;
        }

        public String getFeedback() {
            return feedback;
        }

        @Override
        public String toString() {
            return "{score=" + score + ", isCorrect=" + isCorrect + ", method='" + gradingMethod + "', feedback='"
                    + feedback + "'}";
        }
    }
}
