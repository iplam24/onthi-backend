package com.onthi.v_edu.attempt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback chấm bài bằng GitHub Models (OpenAI-compatible API).
 */
@Service
public class GitHubModelsAiGradingService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubModelsAiGradingService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.github-models.api-key:}")
    private String apiKey;

    @Value("${app.github-models.endpoint:https://models.github.ai/inference/chat/completions}")
    private String endpoint;

    @Value("${app.github-models.model:gpt-4o}")
    private String model;

    @Value("${app.github-models.enabled:true}")
    private boolean enabled;

    public AiGradingResult gradeWithGitHubModels(String questionText, String studentAnswer, String sampleAnswer, double maxScore) {
        if (!enabled) {
            return new AiGradingResult(null, false, "GitHub Models fallback bị tắt", null);
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new AiGradingResult(null, false, "GitHub Models API key chưa được cấu hình", null);
        }

        try {
            String prompt = buildPrompt(questionText, studentAnswer, sampleAnswer, maxScore);
            Map<String, Object> requestBody = buildRequestBody(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("[GITHUB MODELS] Sending grading request to {} model={}", endpoint, model);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);
            if (response == null) {
                return new AiGradingResult(null, false, "GitHub Models trả về kết quả rỗng", null);
            }

            String responseText = extractAssistantText(response);
            logger.info("[GITHUB MODELS] Response text: {}", responseText);
            return parseResponse(responseText, maxScore);
        } catch (HttpClientErrorException.TooManyRequests e) {
            String shortMessage = shortMessage(e.getResponseBodyAsString());
            logger.warn("[GITHUB MODELS] 429 Too Many Requests: {}", shortMessage);
            return new AiGradingResult(null, false, "GitHub Models quá tải/giới hạn gọi", null);
        } catch (HttpClientErrorException e) {
            String shortMessage = shortMessage(e.getResponseBodyAsString());
            logger.warn("[GITHUB MODELS] HTTP error {}: {}", e.getStatusCode(), shortMessage);
            return new AiGradingResult(null, false, "Lỗi gọi GitHub Models: " + shortMessage, null);
        } catch (Exception e) {
            logger.error("[GITHUB MODELS] Unexpected error: {}", e.getMessage());
            return new AiGradingResult(null, false, "Lỗi gọi GitHub Models", null);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("max_tokens", 512);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", "Bạn là giáo viên chấm bài tự luận. Chỉ trả JSON hợp lệ."),
                Map.of("role", "user", "content", prompt)
        ));
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantText(Map<String, Object> response) {
        Object choices = response.get("choices");
        if (choices instanceof List<?> choiceList && !choiceList.isEmpty()) {
            Object first = choiceList.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                Object message = ((Map<String, Object>) firstMap).get("message");
                if (message instanceof Map<?, ?> messageMap) {
                    Object content = ((Map<String, Object>) messageMap).get("content");
                    return content == null ? null : content.toString();
                }
            }
        }
        return null;
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

            return new AiGradingResult(score, isCorrect != null ? isCorrect : score >= maxScore * 0.8, "Chấm bằng GitHub Models", feedback);
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
        - feedback phải ngắn gọn, rõ ràng.
        - Nêu được:
          + điểm mạnh
          + phần còn thiếu
          + phần sai nếu có

        BẮT BUỘC:
        - Chỉ trả về JSON hợp lệ.
        - Không thêm giải thích ngoài JSON.
        - Format JSON:

        {
          "score": number,
          "isCorrect": boolean,
          "feedback": "string"
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

    private String shortMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return "Không có nội dung lỗi";
        String compact = rawBody.replace("\n", " ").replace("\r", " ").trim();
        return compact.length() > 180 ? compact.substring(0, 180) + "..." : compact;
    }

    private Double extractDouble(String json, String key) {
        String value = extractValue(json, key);
        if (value == null) return null;
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
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;

        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
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

