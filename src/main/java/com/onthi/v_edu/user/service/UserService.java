package com.onthi.v_edu.user.service;

import com.onthi.v_edu.common.dto.ApiResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserService {

	record UserInformationRequest(String fullName, String schoolName, Integer levelId, LocalDate dob, String avatar) {
	}

	record BalanceUpdateRequest(java.math.BigDecimal amount, String type) {
	}

	record UserStreakResponse(Integer currentStreak, Integer longestStreak, LocalDate lastActiveDate,
			boolean activeToday, Integer fireLevel) {
	}

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
			boolean enabled,
			UserStreakResponse streak) {
	}

	ApiResponse<UserProfileResponse> getMyProfile();

	ApiResponse<UserProfileResponse> getUserProfile(Integer id);

	ApiResponse<java.util.List<UserProfileResponse>> searchUsers(String query);

	ApiResponse<UserProfileResponse> updateMyInformation(UserInformationRequest request);

	ApiResponse<UserStreakResponse> getMyStreak();

	ApiResponse<UserStreakResponse> checkInMyStreak();

	void recordStudyActivity(Integer userId, LocalDate activityDate);

	ApiResponse<org.springframework.data.domain.Page<UserProfileResponse>> getAllUsers(
			org.springframework.data.domain.Pageable pageable, String query);

	ApiResponse<UserProfileResponse> updateUserStatus(Integer id, boolean enabled);

	ApiResponse<UserProfileResponse> updateUserBalance(Integer id, java.math.BigDecimal amount, String type);

	ApiResponse<UserProfileResponse> updateUserInformationByAdmin(Integer id, UserInformationRequest request);

	Optional<com.onthi.v_edu.user.entity.User> findByUsername(String username);

	Optional<com.onthi.v_edu.user.entity.User> findById(Integer id);

}
