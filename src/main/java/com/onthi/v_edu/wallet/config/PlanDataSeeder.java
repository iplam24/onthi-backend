package com.onthi.v_edu.wallet.config;

import com.onthi.v_edu.wallet.entity.Plan;
import com.onthi.v_edu.wallet.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlanDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PlanDataSeeder.class);
    private final PlanRepository planRepository;

    @Override
    public void run(String... args) {
        if (planRepository.count() == 0) {
            logger.info("[SEEDER] Database plans table is empty. Seeding default plans...");

            Plan free = createPlan("Free", 0, 9999, 5, false, true, false, false, false, false);
            Plan pro = createPlan("Pro", 49000, 30, 9999, true, true, true, true, false, false);
            Plan proMax = createPlan("ProMax", 99000, 30, 9999, true, true, true, true, true, true);

            planRepository.saveAll(List.of(free, pro, proMax));
            logger.info("[SEEDER] Default plans seeded successfully.");
        } else {
            logger.info("[SEEDER] Plans table already has data. Skipping seeding.");
        }
    }

    private Plan createPlan(String name, int price, int days, int aiQuotas, boolean chatbot, boolean grading, boolean stats, boolean customExams, boolean aiHistory, boolean mentor) {
        Plan plan = new Plan();
        plan.setName(name);
        plan.setPrice(BigDecimal.valueOf(price));
        plan.setDurationDays(days);
        plan.setMaxAiQuestionsPerDay(aiQuotas);
        plan.setHasAiChatbot(chatbot);
        plan.setHasAiGrading(grading);
        plan.setHasAdvancedStats(stats);
        plan.setHasCustomExams(customExams);
        plan.setHasAiHistory(aiHistory);
        plan.setIsMentorPlan(mentor);
        return plan;
    }
}
