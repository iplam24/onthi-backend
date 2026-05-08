package com.onthi.v_edu.attempt.scheduler;

import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.attempt.service.AttemptAsyncGradingService;
import com.onthi.v_edu.common.constant.AttemptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler quét các bài thi đang ở trạng thái GRADING để chấm bằng AI.
 * Đảm bảo bài thi luôn được chấm kể cả khi user không online hoặc process async bị ngắt.
 */
@Component
public class AttemptAiGradingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AttemptAiGradingScheduler.class);

    private final AttemptRepository attemptRepository;
    private final AttemptAsyncGradingService attemptAsyncGradingService;

    public AttemptAiGradingScheduler(AttemptRepository attemptRepository,
                                    AttemptAsyncGradingService attemptAsyncGradingService) {
        this.attemptRepository = attemptRepository;
        this.attemptAsyncGradingService = attemptAsyncGradingService;
    }

    // Chạy mỗi 15 giây theo yêu cầu (10-15s)
    @Scheduled(fixedDelay = 15000)
    public void processGradingAttempts() {
        List<Attempt> gradingAttempts = attemptRepository.findByStatus(AttemptStatus.GRADING);
        
        if (!gradingAttempts.isEmpty()) {
            logger.info("[GRADING SCHEDULER] Tìm thấy {} bài thi đang chờ chấm AI", gradingAttempts.size());
            
            for (Attempt attempt : gradingAttempts) {
                try {
                    // Gọi service chấm bài (service đã có check trạng thái và xử lý async nội bộ)
                    // Tuy nhiên scheduler này chạy tuần tự để tránh spam API quá mức
                    attemptAsyncGradingService.gradeAttemptAsync(attempt.getId());
                } catch (Exception e) {
                    logger.error("[GRADING SCHEDULER] Lỗi khi chấm bài ID {}: {}", attempt.getId(), e.getMessage());
                }
            }
        }
    }
}
