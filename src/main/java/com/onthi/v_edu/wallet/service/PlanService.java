package com.onthi.v_edu.wallet.service;

import com.onthi.v_edu.common.constant.UserPlanStatus;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.wallet.entity.Plan;
import com.onthi.v_edu.wallet.entity.UserPlan;
import com.onthi.v_edu.wallet.repository.PlanRepository;
import com.onthi.v_edu.wallet.repository.UserPlanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);
    private final PlanRepository planRepository;
    private final UserPlanRepository userPlanRepository;
    private final WalletService walletService;

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public Optional<UserPlan> getActiveUserPlan(Integer userId) {
        return userPlanRepository.findActivePlanByUserId(userId);
    }

    @Transactional
    public UserPlan purchasePlan(User user, Integer planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Gói cước không tồn tại."));

        logger.info("[PLAN] User {} đang mua gói cước: {}", user.getUsername(), plan.getName());

        userPlanRepository.findActivePlanByUserId(user.getId()).ifPresent(currentPlan -> {
            if (plan.getPrice().compareTo(currentPlan.getPlan().getPrice()) < 0) {
                throw new RuntimeException(
                        "Bạn đang sử dụng gói cước cao cấp hơn. Không thể hạ cấp gói cước trực tiếp.");
            }
        });

        // 2. Trừ tiền từ ví
        walletService.deductBalance(user, plan.getPrice(), "PURCHASE_PLAN_" + plan.getName());

        // 2. Hủy các gói cước cũ đang active (nếu có)
        userPlanRepository.findActivePlanByUserId(user.getId()).ifPresent(oldPlan -> {
            oldPlan.setStatus(UserPlanStatus.CANCELLED);
            userPlanRepository.save(oldPlan);
        });

        // 3. Tạo UserPlan mới
        UserPlan userPlan = new UserPlan();
        userPlan.setUser(user);
        userPlan.setPlan(plan);
        userPlan.setStatus(UserPlanStatus.ACTIVE);
        userPlan.setStartDate(LocalDateTime.now());
        userPlan.setEndDate(LocalDateTime.now().plusDays(plan.getDurationDays()));

        return userPlanRepository.save(userPlan);
    }

    public boolean canAccessFeature(Integer userId, String featureName) {
        Optional<UserPlan> activePlanOpt = getActiveUserPlan(userId);
        if (activePlanOpt.isEmpty()) {
            return false; // Hoặc mặc định cho phép một số tính năng Free
        }

        Plan plan = activePlanOpt.get().getPlan();
        return switch (featureName.toLowerCase()) {
            case "ai_chatbot" -> plan.getHasAiChatbot() != null && plan.getHasAiChatbot();
            case "ai_grading" -> plan.getHasAiGrading() != null && plan.getHasAiGrading();
            case "advanced_stats" -> plan.getHasAdvancedStats() != null && plan.getHasAdvancedStats();
            case "mentor" -> plan.getIsMentorPlan() != null && plan.getIsMentorPlan();
            default -> false;
        };
    }
}
