package com.onthi.v_edu.attempt.service;

import com.onthi.v_edu.attempt.entity.Answer;
import com.onthi.v_edu.question.entity.EssayAnswer;
import org.springframework.stereotype.Service;

/**
 * Service để chấm điểm tự luận (Essay) tự động hoặc bán tự động.
 * 
 * Logic chấm:
 * - Nếu đáp án < 50 ký tự: chấm tự động (0 điểm)
 * - Nếu đáp án >= 50 ký tự: so sánh với đáp án mẫu
 *   + Nếu giống nhau (match): cho đúng (full điểm)
 *   + Nếu không giống: mark isPending=true (cần xử lý hướng khác, ví dụ AI chấm)
 */
@Service
public class EssayGradingService {

    private static final int CHARACTER_THRESHOLD = 50;
    private static final double SIMILARITY_THRESHOLD = 0.8;  // 80% similarity để coi là giống

    /**
     * Chấm điểm tự luận cho một câu trả lời.
     *
     * Logic:
     * - Nếu < 50 ký tự: so sánh với đáp án mẫu (chấm auto)
     *   + Nếu giống: score = full, isCorrect = true
     *   + Nếu khác: score = 0, isCorrect = false
     * - Nếu >= 50 ký tự: mark pending (xử lí sau, ví dụ AI chấm)
     *
     * @param answer        Đối tượng Answer chứa essayAnswer, score, ...
     * @param sampleAnswer  Đáp án mẫu (EssayAnswer entity)
     * @param questionScore Điểm tối đa của câu hỏi
     * @return GradingResult chứa score, isCorrect, isPending, ...
     */
    public GradingResult gradeEssay(Answer answer, EssayAnswer sampleAnswer, double questionScore) {
        if (answer == null) {
            throw new IllegalArgumentException("Answer không được null");
        }

        String essayAnswer = answer.getEssayAnswer();
        
        // ...existing code...
        if (essayAnswer == null || essayAnswer.trim().isEmpty()) {
            return new GradingResult(
                    0d,                          // score
                    false,                       // isCorrect
                    false,                       // isPending (không cần chờ chấm)
                    "Đáp án trống",              // reason
                    null                         // manualGradeRequired
            );
        }

        String trimmedAnswer = essayAnswer.trim();
        int answerLength = trimmedAnswer.length();

        // Nếu đáp án < 50 ký tự: so sánh với đáp án mẫu (chấm auto)
        if (answerLength < CHARACTER_THRESHOLD) {
            // Nếu không có đáp án mẫu: chấm 0
            if (sampleAnswer == null || sampleAnswer.getSampleAnswer() == null) {
                return new GradingResult(
                        0d,
                        false,
                        false,
                        String.format("Đáp án (%d ký tự), không có đáp án mẫu để so sánh",
                                      answerLength),
                        null
                );
            }

            // So sánh với đáp án mẫu
            String sampleText = sampleAnswer.getSampleAnswer().trim();
            boolean isMatching = compareAnswers(trimmedAnswer, sampleText);

            if (isMatching) {
                // Đáp án giống với đáp án mẫu: cho đúng (full điểm)
                return new GradingResult(
                        questionScore,
                        true,
                        false,
                        String.format("Đáp án (%d ký tự) khớp với mẫu",
                                      answerLength),
                        null
                );
            } else {
                // Đáp án không khớp: chấm 0
                return new GradingResult(
                        0d,
                        false,
                        false,
                        String.format("Đáp án (%d ký tự) không khớp với mẫu",
                                      answerLength),
                        null
                );
            }
        }

        // Nếu đáp án >= 50 ký tự: pending (xử lí sau)
        return new GradingResult(
                null,
                null,
                true,
                String.format("Đáp án dài (%d ký tự >= %d), cần xử lí sau",
                              answerLength, CHARACTER_THRESHOLD),
                true
        );
    }

    /**
     * Kiểm tra xem đáp án có nên chấm tự động không (< 50 ký tự).
     *
     * @param essayAnswer Nội dung đáp án
     * @return true nếu nên chấm tự động, false nếu cần xử lý hướng khác
     */
    public boolean shouldAutoGrade(String essayAnswer) {
        if (essayAnswer == null) {
            return true; // Chấm tự động (0 điểm)
        }
        return essayAnswer.trim().length() < CHARACTER_THRESHOLD;
    }

    /**
     * Lấy ngưỡng ký tự để quyết định chấm tự động.
     *
     * @return Ngưỡng ký tự (50)
     */
    public int getCharacterThreshold() {
        return CHARACTER_THRESHOLD;
    }

    /**
     * DTO kết quả chấm điểm tự luận.
     */
    public static class GradingResult {
        private final Double score;                  // Điểm (null nếu cần chấm thủ công)
        private final Boolean isCorrect;             // Đúng/sai (null nếu cần chấm thủ công)
        private final boolean isPending;             // Có cần chấm thủ công không
        private final String reason;                 // Lý do chấm
        private final Boolean manualGradeRequired;   // Flag cho biết cần chấm thủ công

        public GradingResult(Double score, Boolean isCorrect, boolean isPending,
                           String reason, Boolean manualGradeRequired) {
            this.score = score;
            this.isCorrect = isCorrect;
            this.isPending = isPending;
            this.reason = reason;
            this.manualGradeRequired = manualGradeRequired;
        }

        public Double getScore() {
            return score;
        }

        public Boolean getIsCorrect() {
            return isCorrect;
        }

        public boolean isPending() {
            return isPending;
        }

        public String getReason() {
            return reason;
        }

        public Boolean getManualGradeRequired() {
            return manualGradeRequired;
        }

        @Override
        public String toString() {
            return "GradingResult{" +
                    "score=" + score +
                    ", isCorrect=" + isCorrect +
                    ", isPending=" + isPending +
                    ", reason='" + reason + '\'' +
                    ", manualGradeRequired=" + manualGradeRequired +
                    '}';
        }
    }

    /**
     * So sánh hai đáp án để kiểm tra xem chúng có khớp nhau không.
     * 
     * Chiến lược so sánh:
     * 1. Normalize: chuyển hoa thường, loại bỏ khoảng trắng thừa, loại bỏ dấu câu
     * 2. Exact match: so sánh trực tiếp
     * 3. Similarity check: sử dụng Levenshtein distance >= 80%
     *
     * @param studentAnswer Đáp án của học sinh
     * @param sampleAnswer  Đáp án mẫu
     * @return true nếu khớp, false nếu không khớp
     */
    public boolean compareAnswers(String studentAnswer, String sampleAnswer) {
        if (studentAnswer == null || sampleAnswer == null) {
            return false;
        }

        // Normalize: chuyển hoa thường, remove extra spaces
        String normalized1 = normalizeText(studentAnswer);
        String normalized2 = normalizeText(sampleAnswer);

        // 1. Kiểm tra exact match trước
        if (normalized1.equalsIgnoreCase(normalized2)) {
            return true;
        }

        // 2. Kiểm tra độ tương đồng (similarity) >= 80%
        double similarity = calculateSimilarity(normalized1, normalized2);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    /**
     * Normalize text: loại bỏ khoảng trắng thừa, chuyển hoa thường, loại bỏ dấu câu.
     *
     * @param text Text cần normalize
     * @return Text đã được normalize
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        // Loại bỏ khoảng trắng đầu-cuối, chuyển hoa thường, replace multiple spaces thành 1
        return text.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[.,!?;:]", "");  // remove punctuation
    }

    /**
     * Tính độ tương đồng (similarity) giữa 2 chuỗi bằng Levenshtein distance.
     * 
     * Formula: similarity = 1 - (levenshtein_distance / max_length)
     *
     * @param s1 Chuỗi 1
     * @param s2 Chuỗi 2
     * @return Giá trị similarity từ 0 đến 1 (1 = hoàn toàn giống)
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) {
            return 1.0;  // Cả hai đều rỗng
        }

        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Tính Levenshtein distance (số ký tự cần thay đổi để chuyển s1 thành s2).
     * 
     * Sử dụng dynamic programming.
     *
     * @param s1 Chuỗi 1
     * @param s2 Chuỗi 2
     * @return Levenshtein distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,       // deletion
                        dp[i][j - 1] + 1),      // insertion
                        dp[i - 1][j - 1] + cost // substitution
                );
            }
        }

        return dp[len1][len2];
    }
}

