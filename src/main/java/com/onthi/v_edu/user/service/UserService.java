package com.onthi.v_edu.user.service;

import com.onthi.v_edu.common.dto.ApiResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UserService {

	record UserInformationRequest(String fullName, String schoolName, Integer levelId, LocalDate dob, String avatar) {}

	record UserStreakResponse(Integer currentStreak, Integer longestStreak, LocalDate lastActiveDate, boolean activeToday, Integer fireLevel) {}

	record UserProfileResponse(Integer id,
							 String username,
							 String email,
							 String roleName,
							 String fullName,
							 String schoolName,
							 Integer levelId,
							 String levelName,
							 LocalDate dob,
							 String avatar,
							 LocalDateTime createdAt,
							 LocalDateTime updatedAt,
							 java.math.BigDecimal balance,
							 UserStreakResponse streak) {}

	ApiResponse<UserProfileResponse> getMyProfile();

	ApiResponse<UserProfileResponse> updateMyInformation(UserInformationRequest request);

	ApiResponse<UserStreakResponse> getMyStreak();

	ApiResponse<UserStreakResponse> checkInMyStreak();

	void recordStudyActivity(Integer userId, LocalDate activityDate);

}
