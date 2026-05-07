package com.onthi.v_edu.attempt.scheduler;

import com.onthi.v_edu.attempt.service.AttemptService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttemptAutoSubmitScheduler {

    private final AttemptService attemptService;

    public AttemptAutoSubmitScheduler(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @Scheduled(fixedDelayString = "${app.attempt.auto-submit-interval-ms:60000}")
    public void autoSubmitExpiredAttempts() {
        attemptService.expireOverdueAttempts();
    }
}

