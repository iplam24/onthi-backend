package com.onthi.v_edu.ai.service;

import com.onthi.v_edu.common.ai.GitHubModelsClientService;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.statistics.dto.StudentEvaluationResponse;
import com.onthi.v_edu.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(AiRecommendationService.class);
    private final GitHubModelsClientService aiClientService;
    private final StatisticsService statisticsService;

    public String generatePersonalizedLearningPath() {
        ApiResponse<StudentEvaluationResponse> evaluationResponse = statisticsService.getMyStudentEvaluation(null);
        if (evaluationResponse == null || evaluationResponse.getData() == null) {
            return "Không có dữ liệu học tập để phân tích.";
        }

        StudentEvaluationResponse data = evaluationResponse.getData();

        String prompt = String.format(
                """
                        Bạn là một chuyên gia tư vấn giáo dục AI. Hãy dựa vào dữ liệu học tập của học sinh dưới đây để viết một LỘ TRÌNH HỌC TẬP CÁ NHÂN HÓA.

                        DỮ LIỆU HỌC TẬP:
                        - Tên: %s
                        - Điểm trung bình: %.2f
                        - Điểm gần nhất: %.2f
                        - Điểm kiến thức: %.2f
                        - Điểm tốc độ: %.2f
                        - Điểm tiến bộ: %.2f
                        - Điểm kỷ luật: %.2f
                        - Điểm tổng thể: %.2f (%s)

                        THẾ MẠNH:
                        %s

                        ĐIỂM YẾU:
                        %s

                        CÁC CHỦ ĐỀ YẾU NHẤT:
                        %s

                        NHIỆM VỤ:
                        Hãy viết một lộ trình học tập chi tiết và truyền cảm hứng bao gồm:
                        1. Nhận xét tổng quan về phong độ hiện tại.
                        2. Lộ trình cụ thể trong 7 ngày tới (Cần tập trung vào chủ đề nào, môn nào).
                        3. Lời khuyên về cách phân bổ thời gian và cải thiện tốc độ làm bài (nếu cần).
                        4. Một câu nói truyền động lực phù hợp với tình trạng hiện tại.

                        Trả về bằng Tiếng Việt, sử dụng Markdown để trình bày đẹp mắt.
                        """,
                data.getFullName() != null ? data.getFullName() : data.getUsername(),
                data.getAverageScore(),
                data.getLatestScore(),
                data.getKnowledgeScore(),
                data.getSpeedScore(),
                data.getProgressScore(),
                data.getDisciplineScore(),
                data.getOverallScore(),
                data.getPerformanceLabel(),
                String.join("\n", data.getStrengths()),
                String.join("\n", data.getWeaknesses()),
                data.getTopicEvaluations().stream()
                        .filter(t -> t.getAccuracyRate() < 60)
                        .map(t -> t.getTopicName() + " (" + t.getAccuracyRate() + "%)")
                        .limit(3)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Chưa xác định"));

        logger.info("[AI RECOMMENDATION] Generating personalized learning path using GitHub Models for user: {}",
                data.getUsername());
        return aiClientService.generateContent(prompt, "Bạn là chuyên gia tư vấn giáo dục AI.");
    }
}
