package com.onthi.v_edu.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalUsers;
    private long newUsersToday;
    private long totalQuestions;
    private long totalExams;
    private long totalAttempts;
    private long totalSubjects;
    private long totalLevels;
    private long totalTopics;
}
