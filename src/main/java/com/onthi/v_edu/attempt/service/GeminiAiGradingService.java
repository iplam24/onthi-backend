package com.onthi.v_edu.attempt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service để gọi Google Gemini AI chấm bài tự luận dài (>= 50 ký tự).
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

    public AiGradingResult gradeWithGemini(String questionText, String studentAnswer, String sampleAnswer, double maxScore) {
        if (!geminiEnabled) {
            logger.warn("Gemini AI grading is disabled");
            return new AiGradingResult(null, false, "Gemini AI chấm bị disable", null);
        }

        if (geminiApiKey == null || geminiApiKey.isEmpty() || geminiApiKey.contains("YOUR_")) {
            logger.error("Gemini API key not configured");
            return new AiGradingResult(null, false, "Gemini API key chưa được cấu hình", null);
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
                return new AiGradingResult(null, false, "Gemini API trả về kết quả rỗng", null);
            }

            String responseText = extractResponseText(response);
            logger.info("[GEMINI] Response text: {}", responseText);

            AiGradingResult result = parseGeminiResponse(responseText, maxScore);
            return result;

        } catch (HttpClientErrorException.TooManyRequests e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && responseBody.contains("RESOURCE_EXHAUSTED")) {
                logger.warn("[GEMINI] Quota depleted (RESOURCE_EXHAUSTED)");
                return new AiGradingResult(null, false, "Gemini hết credit (RESOURCE_EXHAUSTED)", null);
            }
            logger.warn("[GEMINI] 429 Too Many Requests");
            return new AiGradingResult(null, false, "Gemini đang quá tải (429)", null);
        } catch (HttpClientErrorException e) {
            String shortMessage = extractShortApiMessage(e.getResponseBodyAsString());
            logger.warn("[GEMINI] HTTP error {}: {}", e.getStatusCode(), shortMessage);
            return new AiGradingResult(null, false, "Lỗi gọi Gemini: " + shortMessage, null);
        } catch (Exception e) {
            // Keep logs concise for unexpected failures to avoid flooding runtime logs.
            logger.error("[GEMINI] Unexpected error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Lỗi gọi Gemini", null);
        }
    }

    private String extractShortApiMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "Không có nội dung lỗi từ API";
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
            if (candidates instanceof java.util.List<?> list && !list.isEmpty()) {
                Map<String, Object> candidate = (Map<String, Object>) list.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                java.util.List<?> parts = (java.util.List<?>) content.get("parts");
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
        Bạn là giáo viên chấm bài tự luận nghiêm túc và công bằng.

        NHIỆM VỤ:
        - Chấm bài dựa trên đáp án mẫu.
        - Cho điểm theo mức độ đúng và đầy đủ của bài làm.
        - Chỉ chấm những ý học sinh thực sự viết.
        - Không cộng điểm cho nội dung không tồn tại trong bài.
        - Làm ít chấm ít, làm nhiều chấm nhiều.
        - Nếu chỉ làm một phần thì chỉ cho điểm phần đó.
        - Nếu chỉ có mở bài thì chỉ cho điểm mở bài.
        - Nếu có mở bài + vài ý thân bài thì cộng điểm tương ứng.
        - Không yêu cầu học sinh phải giống 100%% đáp án mẫu.
        - Chấp nhận cách diễn đạt khác nhưng cùng ý nghĩa.
        - Ưu tiên phát hiện ý đúng, từ khóa đúng, luận điểm đúng.
        - Không chấm quá dễ.
        - Không chấm quá khắt khe.
        - Không cho điểm tối đa nếu bài thiếu ý quan trọng.
        - Nếu học sinh viết lan man, sai trọng tâm hoặc bịa nội dung thì trừ điểm phù hợp.
        - Nếu bài chỉ sao chép một phần nhỏ đáp án thì chỉ cho điểm tương ứng phần đó.

        QUY TẮC CHẤM:
        1. Xác định các ý chính trong đáp án mẫu.
        2. Chia điểm theo từng ý.
        3. So sánh bài học sinh với từng ý.
        4. Ý nào đúng thì cộng điểm ý đó.
        5. Ý nào thiếu thì không cộng điểm.
        6. Ý sai hoặc trái nghĩa thì trừ nhẹ điểm nếu nghiêm trọng.
        7. Có thể cho điểm lẻ như 0.25, 0.5, 0.75...
        8. Tổng điểm tối đa là %.2f.

        TRƯỜNG HỢP MÔN VĂN:
        - Có thể chấm theo:
          + mở bài
          + thân bài
          + kết bài
          + phân tích nội dung
          + nghệ thuật
          + cảm nhận cá nhân hợp lý
        - Học sinh làm tới đâu chấm tới đó.
        - Không bắt buộc đúng từng chữ.
        - Nếu phân tích đúng ý thơ, tác giả, hình tượng, biện pháp nghệ thuật thì cộng điểm tương ứng.
        - Nếu chỉ viết mở bài hoặc giới thiệu tác giả thì chỉ cho phần điểm phù hợp.

        PHẢN HỒI:
        - feedback phải chi tiết, mang tính xây dựng.
        - Phải bao gồm các phần:
          + Nhận xét tổng quát (Bài làm đạt yêu cầu hay chưa).
          + Ưu điểm (Những ý đúng, kỹ năng viết, cách diễn đạt tốt).
          + Nhược điểm/Thiếu sót (Những ý còn thiếu so với đáp án mẫu, lỗi lập luận, lỗi trình bày).
          + Gợi ý cải thiện (Cách để đạt điểm tối đa).
        - Sử dụng tiếng Việt tự nhiên, chuyên nghiệp.
        - Không trả lời quá ngắn gọn kiểu "Tốt" hay "Thiếu ý".

        BẮT BUỘC:
        - Chỉ trả về JSON hợp lệ.
        - Không thêm giải thích ngoài JSON.
        - Format JSON:

        {
          "score": number,
          "isCorrect": boolean,
          "feedback": "string (bao gồm các phần nhận xét chi tiết, xuống dòng bằng \\n)"
        }

        CÂU HỎI:
        %s

        ĐÁP ÁN MẪU:
        %s

        BÀI LÀM HỌC SINH:
        %s
        """,
                maxScore,
                questionText,
                sampleAnswer,
                studentAnswer
        );
    }

    private AiGradingResult parseGeminiResponse(String responseText, double maxScore) {
        if (responseText == null || responseText.isEmpty()) {
            return new AiGradingResult(null, false, "Gemini trả về kết quả rỗng", null);
        }

        try {
            String cleaned = responseText.replaceAll("```.*?```", "").replaceAll("```", "").trim();
            
            Double score = extractScore(cleaned);
            if (score == null) return new AiGradingResult(null, false, "Không thể parse score", null);
            if (score > maxScore) score = maxScore;

            Boolean isCorrect = extractIsCorrect(cleaned);
            String feedback = extractFeedback(cleaned);

            return new AiGradingResult(score, isCorrect != null ? isCorrect : (score >= maxScore * 0.8), "Chấm bằng Gemini AI", feedback);
        } catch (Exception e) {
            logger.error("[GEMINI] Parse error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Lỗi parse: " + e.getMessage(), null);
        }
    }

    private Double extractScore(String json) {
        String val = extractJsonValue(json, "score");
        return val != null ? Double.parseDouble(val.trim()) : null;
    }

    private Boolean extractIsCorrect(String json) {
        String val = extractJsonValue(json, "isCorrect");
        return val != null ? Boolean.parseBoolean(val.trim()) : null;
    }

    private String extractFeedback(String json) {
        return extractJsonValue(json, "feedback");
    }

    private String extractJsonValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;

        char c = json.charAt(start);
        if (c == '"') {
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
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

