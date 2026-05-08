package com.onthi.v_edu.attempt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de goi Google Gemini AI cham bai tu luan.
 * Ho tro cham don le va cham theo lo (batch).
 */
@Service
public class GeminiAiGradingService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAiGradingService.class);
    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.gemini.enabled:true}")
    private boolean geminiEnabled;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class BatchItem {
        private final Integer questionId;
        private final String questionText;
        private final String studentAnswer;
        private final String sampleAnswer;
        private final double maxScore;
        private final String questionType;
        private final List<String> options;

        public BatchItem(Integer questionId, String questionText, String studentAnswer, String sampleAnswer, double maxScore, String questionType, List<String> options) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.studentAnswer = studentAnswer;
            this.sampleAnswer = sampleAnswer;
            this.maxScore = maxScore;
            this.questionType = questionType;
            this.options = options;
        }

        public Integer getQuestionId() { return questionId; }
        public String getQuestionText() { return questionText; }
        public String getStudentAnswer() { return studentAnswer; }
        public String getSampleAnswer() { return sampleAnswer; }
        public double getMaxScore() { return maxScore; }
        public String getQuestionType() { return questionType; }
        public List<String> getOptions() { return options; }
    }

    public AiGradingResult gradeWithGemini(String questionText, String studentAnswer, String sampleAnswer, double maxScore) {
        if (!geminiEnabled) {
            logger.warn("Gemini AI grading is disabled");
            return new AiGradingResult(null, false, "Nhận xét của giáo viên", null);
        }

        if (geminiApiKey == null || geminiApiKey.isEmpty() || geminiApiKey.contains("YOUR_")) {
            logger.error("Gemini API key not configured");
            return new AiGradingResult(null, false, "Gemini API key chua duoc cau hinh", null);
        }

        try {
            String prompt = buildGradingPrompt(questionText, studentAnswer, sampleAnswer, maxScore);
            logger.info("[GEMINI] Sending prompt to Gemini API...");

            Map<String, Object> requestBody = buildRequestBody(prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String fullUrl = buildGeminiUrl();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);

            if (response == null) {
                logger.error("[GEMINI] Empty response from API");
                return new AiGradingResult(null, false, "Gemini API tra ve ket qua rong", null);
            }

            String responseText = extractResponseText(response);
            logger.info("[GEMINI] Response text received.");

            return parseGeminiResponse(responseText, maxScore);

        } catch (HttpClientErrorException.TooManyRequests e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && responseBody.contains("RESOURCE_EXHAUSTED")) {
                logger.warn("[GEMINI] Quota depleted (RESOURCE_EXHAUSTED)");
                return new AiGradingResult(null, false, "Gemini het credit (RESOURCE_EXHAUSTED)", null);
            }
            logger.warn("[GEMINI] 429 Too Many Requests");
            return new AiGradingResult(null, false, "Gemini dang qua tai (429)", null);
        } catch (HttpClientErrorException e) {
            String shortMessage = extractShortApiMessage(e.getResponseBodyAsString());
            logger.warn("[GEMINI] HTTP error {}: {}", e.getStatusCode(), shortMessage);
            return new AiGradingResult(null, false, "Loi goi Gemini: " + shortMessage, null);
        } catch (Exception e) {
            logger.error("[GEMINI] Unexpected error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Loi goi Gemini", null);
        }
    }

    public Map<Integer, AiGradingResult> gradeBatchWithGemini(String examTitle, List<BatchItem> items) {
        if (!geminiEnabled || items == null || items.isEmpty()) {
            return new HashMap<>();
        }

        if (geminiApiKey == null || geminiApiKey.isEmpty() || geminiApiKey.contains("YOUR_")) {
            logger.error("Gemini API key not configured");
            return new HashMap<>();
        }

        try {
            String prompt = buildBatchGradingPrompt(examTitle, items);
            logger.info("[GEMINI BATCH] Sending batch prompt ({} items) to Gemini API...", items.size());

            Map<String, Object> requestBody = buildRequestBody(prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String fullUrl = buildGeminiUrl();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);

            if (response == null) {
                logger.error("[GEMINI BATCH] Empty response from API");
                return new HashMap<>();
            }

            String responseText = extractResponseText(response);
            logger.info("[GEMINI BATCH] Response text received.");

            return parseBatchGeminiResponse(responseText, items);

        } catch (Exception e) {
            logger.error("[GEMINI BATCH] Unexpected error: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String extractShortApiMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "Khong co noi dung loi tu API";
        }
        String compact = rawBody.replace("\n", " ").replace("\r", " ").trim();
        int max = 180;
        return compact.length() > max ? compact.substring(0, max) + "..." : compact;
    }

    private String buildGeminiUrl() {
        String model = (geminiModel == null || geminiModel.isBlank()) ? "gemini-1.5-flash" : geminiModel.trim();
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);
        return String.format(GEMINI_API_URL_TEMPLATE, encodedModel) + "?key=" + geminiApiKey;
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[]{part});
        body.put("contents", new Object[]{content});
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(Map<String, Object> response) {
        try {
            Object candidates = response.get("candidates");
            if (candidates instanceof List<?> list && !list.isEmpty()) {
                Map<String, Object> candidate = (Map<String, Object>) list.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                List<?> parts = (List<?>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    Map<String, Object> part = (Map<String, Object>) parts.get(0);
                    return part.get("text").toString();
                }
            }
        } catch (Exception e) {
            logger.warn("[GEMINI] Error extracting response: {}", e.getMessage());
        }
        return null;
    }

    private String buildGradingPrompt(String questionText, String studentAnswer, String sampleAnswer, double maxScore) {
        return String.format("""
        Ban la giao vien cham bai tu luan nghiem tuc va cong bang.

        NHIEM VU:
        - Cham bai dua tren dap an mau.
        - Cho diem theo muc do dung va day du cua bai lam.
        - Phan hoi chi tiet (Nhan xet, Uu diem, Nhuoc diem, Goi y).
        
        BAT BUOC:
        - Chi tra ve JSON hop le.
        - Format JSON:
        {
          "score": number,
          "isCorrect": boolean,
          "feedback": "string"
        }

        MAX_SCORE: %.2f
        CAU HOI: %s
        DAP AN MAU: %s
        BAI LAM: %s
        """, maxScore, questionText, sampleAnswer, studentAnswer);
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
            (item.getOptions() != null && !item.getOptions().isEmpty()) ? String.join(", ", item.getOptions()) : "N/A",
            item.getSampleAnswer() != null ? item.getSampleAnswer() : "(N/A)",
            item.getStudentAnswer()));
        }

        return String.format("""
        Bạn là một giáo viên chuyên nghiệp và công bằng đang chấm bài cho đề thi: "%s".
        
        NHIỆM VỤ:
        Hãy chấm điểm danh sách các câu hỏi dưới đây. Mỗi câu hỏi có một yêu cầu (QUESTION_TITLE) và đáp án mẫu (SAMPLE_ANSWER) riêng biệt.
        
        QUY TẮC CHẤM:
        1. Đối với TRẮC NGHIỆM (MCQ):
           - So sánh STUDENT_ANSWER với SAMPLE_ANSWER. Nếu đúng hoàn toàn thì cho điểm tối đa.
           
        2. Đối với TỰ LUẬN (ESSAY):
           - So sánh bài làm của học sinh với QUESTION_TITLE và SAMPLE_ANSWER.
           - Cho điểm dựa trên mức độ hoàn thành yêu cầu đề bài.
           - QUAN TRỌNG: Phản hồi (feedback) phải RIÊNG BIỆT và CHI TIẾT cho từng câu hỏi. Không được dùng các câu trả lời chung chung hoặc giống nhau cho các câu hỏi khác nhau.
        
        YÊU CẦU ĐỊNH DẠNG:
        - Trả về DUY NHẤT một mảng JSON (hoặc object có key "results") chứa các đối tượng kết quả.
        - Mỗi đối tượng phải có cấu trúc:
        {
          "questionId": number,
          "score": number,
          "isCorrect": boolean,
          "feedback": "string"
        }
        
        DANH SÁCH CÂU HỎI CẦN CHẤM:
        %s
        """, examTitle, questionsBlock.toString());
    }

    private AiGradingResult parseGeminiResponse(String responseText, double maxScore) {
        if (responseText == null || responseText.isEmpty()) {
            return new AiGradingResult(null, false, "Gemini tra ve ket qua rong", null);
        }
        try {
            String cleaned = responseText.replaceAll("```json", "").replaceAll("```", "").trim();
            Map<String, Object> map = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
            
            Double score = Double.valueOf(map.getOrDefault("score", 0.0).toString());
            if (score > maxScore) score = maxScore;
            
            Object isCorrectObj = map.get("isCorrect");
            Boolean isCorrect = isCorrectObj instanceof Boolean ? (Boolean) isCorrectObj : null;
            if (isCorrect == null) isCorrect = score >= maxScore * 0.8;
            
            String feedback = (String) map.getOrDefault("feedback", "");

            return new AiGradingResult(score, isCorrect, "Nhận xét của giáo viên", feedback);
        } catch (Exception e) {
            logger.error("[GEMINI] Parse error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Loi parse JSON", null);
        }
    }

    private Map<Integer, AiGradingResult> parseBatchGeminiResponse(String responseText, List<BatchItem> items) {
        Map<Integer, AiGradingResult> results = new HashMap<>();
        if (responseText == null || responseText.isEmpty()) return results;

        try {
            String cleaned = responseText.replaceAll("```json", "").replaceAll("```", "").trim();
            
            List<Map<String, Object>> rawResults = null;
            Object parsed = objectMapper.readValue(cleaned, Object.class);
            
            if (parsed instanceof List) {
                rawResults = (List<Map<String, Object>>) parsed;
            } else if (parsed instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) parsed;
                if (map.containsKey("results") && map.get("results") instanceof List) {
                    rawResults = (List<Map<String, Object>>) map.get("results");
                } else if (map.containsKey("questionId")) {
                    rawResults = List.of(map);
                }
            }

            if (rawResults == null) {
                logger.error("[GEMINI BATCH] Không tìm thấy mảng kết quả trong JSON: {}", cleaned);
                return results;
            }

            Map<Integer, BatchItem> itemMap = items.stream().collect(Collectors.toMap(BatchItem::getQuestionId, it -> it));

            for (Map<String, Object> raw : rawResults) {
                try {
                    Object qIdObj = raw.get("questionId");
                    if (qIdObj == null) continue;
                    
                    Integer qId = Integer.valueOf(qIdObj.toString());
                    Double score = Double.valueOf(raw.getOrDefault("score", 0.0).toString());
                    
                    Object isCorrectObj = raw.get("isCorrect");
                    Boolean isCorrect = isCorrectObj instanceof Boolean ? (Boolean) isCorrectObj : null;
                    
                    String feedback = (String) raw.getOrDefault("feedback", "");

                    BatchItem item = itemMap.get(qId);
                    double maxScore = item != null ? item.getMaxScore() : 1.0;
                    if (score > maxScore) score = maxScore;

                    if (isCorrect == null) {
                        isCorrect = score >= maxScore * 0.8;
                    }

                    results.put(qId, new AiGradingResult(score, isCorrect, "Nhận xét của giáo viên", feedback));
                } catch (Exception e) {
                    logger.warn("[GEMINI BATCH] Lỗi parse từng item: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("[GEMINI BATCH] Parse error: {}", e.getMessage());
        }
        return results;
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

        public Double getScore() { return score; }
        public Boolean getIsCorrect() { return isCorrect; }
        public String getGradingMethod() { return gradingMethod; }
        public String getFeedback() { return feedback; }

        @Override
        public String toString() {
            return "{score=" + score + ", isCorrect=" + isCorrect + ", method='" + gradingMethod + "', feedback='" + feedback + "'}";
        }
    }
}
