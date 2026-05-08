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

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.github-models.api-key:}")
    private String apiKey;

    @Value("${app.github-models.endpoint:https://models.github.ai/inference/chat/completions}")
    private String endpoint;

    @Value("${app.github-models.model:gpt-4o}")
    private String model;

    @Value("${app.github-models.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                Map.of("role", "user", "content", prompt)));
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

    public Map<Integer, AiGradingResult> gradeBatchWithGitHubModels(String examTitle, List<BatchItem> items) {
        if (!enabled || items == null || items.isEmpty()) {
            return new HashMap<>();
        }
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("[GITHUB MODELS BATCH] API key chưa được cấu hình");
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
                Map<String, Object> requestBody = buildRequestBody(prompt);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey.trim());
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                logger.info("[GITHUB MODELS BATCH] Sending chunk {}/{} ({} items)", chunkCount, chunks.size(),
                        truncatedChunk.size());
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(endpoint, request, Map.class);
                if (response != null) {
                    String responseText = extractAssistantText(response);
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
                - KHÔNG ưu tiên văn phong nếu nội dung sai.
                - KHÔNG suy diễn thêm ý ngoài đáp án.

                =========================
                QUY TẮC CHẤM QUAN TRỌNG
                =========================

                1. Với môn TOÁN / LÝ / HÓA:
                - Ưu tiên kết quả đúng.
                - Kiểm tra công thức, phép tính, lập luận.
                - Nếu đáp án cuối sai nhưng có hướng làm đúng:
                  cho điểm một phần hợp lý.
                - Không cho điểm nếu lập luận sai bản chất.

                2. Với môn SỬ / ĐỊA / GDCD:
                - Ưu tiên tính chính xác của sự kiện, mốc thời gian,
                  địa danh, khái niệm.
                - Không chấp nhận thông tin bịa hoặc sai fact.
                - Nếu học sinh diễn đạt khác đáp án mẫu nhưng đúng kiến thức:
                  vẫn cho điểm.

                3. Với môn VĂN / TIẾNG ANH:
                - Đánh giá:
                  + đúng nội dung
                  + lập luận
                  + diễn đạt
                  + tính liên kết
                - Không yêu cầu giống hoàn toàn đáp án mẫu.

                4. Với câu hỏi ngắn:
                - Chỉ cần đúng ý chính là đạt điểm cao.

                5. Với mọi môn:
                - Nếu bài làm bỏ trống:
                  score = 0
                - Nếu bài làm hoàn toàn sai:
                  score gần 0
                - Nếu đúng một phần:
                  cho điểm tương ứng mức độ đúng.
                - Tuyệt đối không luôn cho điểm tối đa.

                =========================
                THANG ĐIỂM
                =========================

                - Điểm nằm trong khoảng 0 -> %.2f
                - Có thể dùng số lẻ.
                - Không làm tròn tùy tiện.

                =========================
                ĐỊNH DẠNG PHẢN HỒI
                =========================

                Chỉ trả JSON hợp lệ:

                {
                  "score": number,
                  "isCorrect": boolean,
                  "feedback": "string"
                }

                Trong đó:
                - score: điểm số
                - isCorrect:
                    true nếu đạt >= 80%% số điểm
                    false nếu dưới 80%%
                - feedback:
                    nhận xét ngắn gọn, rõ ràng,
                    nêu đúng/sai ở đâu.

                =========================
                DỮ LIỆU CẦN CHẤM
                =========================

                MAX_SCORE: %.2f

                QUESTION:
                %s

                SAMPLE_ANSWER:
                %s

                STUDENT_ANSWER:
                %s
                """,
                maxScore,
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

                        1. Với môn TOÁN / TỰ NHIÊN:
                           - Ưu tiên tính chính xác tuyệt đối của kết quả và logic giải bài.
                           - Nếu sai bản chất hoặc sai công thức => 0 điểm hoặc điểm rất thấp dù viết dài.
                           
                        2. Với môn XÃ HỘI (Sử, Địa, GDCD):
                           - Ưu tiên các "Key Fact": mốc thời gian, sự kiện, địa danh, con số.
                           - Sai thông tin lịch sử/địa lý cơ bản => Trừ điểm nặng.
                           
                        3. Với môn VĂN HỌC / NGÔN NGỮ:
                           - Đánh giá sự hiểu bài thông qua luận điểm và dẫn chứng.
                           - Phải chỉ rõ học sinh làm đúng ý nào, thiếu ý nào hoặc lạc đề ở đâu.
                           
                        4. QUY TẮC CHUNG:
                           - PHẢN HỒI (feedback) phải RIÊNG BIỆT cho từng câu, không trùng lặp.
                           - Nếu bài làm trống hoặc quá ngắn (<10 ký tự cho tự luận) => 0 điểm.
                           - Không tự suy diễn ý định của học sinh. Chỉ chấm dựa trên những gì đã viết.
                           - Điểm số (score) phải phản ánh đúng năng lực, không cho điểm "khuyến khích" nếu nội dung sai.

                        YÊU CẦU ĐỊNH DẠNG:
                        - Trả về DUY NHẤT một mảng JSON (hoặc object có key "results").
                        - Mỗi đối tượng:
                        {
                          "questionId": number,
                          "score": number (0 -> MAX_SCORE),
                          "isCorrect": boolean (true nếu score >= 80%% MAX_SCORE),
                          "feedback": "string (chi tiết, nêu rõ ưu/nhược điểm)"
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
            Object parsed = objectMapper.readValue(cleaned, Object.class);

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
