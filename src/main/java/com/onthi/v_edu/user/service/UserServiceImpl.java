package com.onthi.v_edu.user.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.learning.entity.Level;
import com.onthi.v_edu.learning.repository.LevelRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.entity.UserInformation;
import com.onthi.v_edu.user.entity.UserStudyStreak;
import com.onthi.v_edu.user.repository.UserInformationRepository;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.user.repository.UserStudyStreakRepository;
import com.onthi.v_edu.wallet.entity.Wallet;
import com.onthi.v_edu.wallet.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserInformationRepository userInformationRepository;
    private final UserStudyStreakRepository userStudyStreakRepository;
    private final LevelRepository levelRepository;
    private final WalletRepository walletRepository;

    public UserServiceImpl(UserRepository userRepository,
                           UserInformationRepository userInformationRepository,
                           UserStudyStreakRepository userStudyStreakRepository,
                           LevelRepository levelRepository,
                           WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.userInformationRepository = userInformationRepository;
        this.userStudyStreakRepository = userStudyStreakRepository;
        this.levelRepository = levelRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getMyProfile() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem hồ sơ!");
        }

        UserInformation userInformation = userInformationRepository.findByUser_Id(currentUser.getId()).orElse(null);
        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(currentUser.getId()).orElse(null);

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy hồ sơ người dùng thành công!", toUserProfileResponse(currentUser, userInformation, streak));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfile(Integer id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy người dùng!");
        }

        UserInformation userInformation = userInformationRepository.findByUser_Id(user.getId()).orElse(null);
        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(user.getId()).orElse(null);

        UserProfileResponse response = toUserProfileResponse(user, userInformation, streak);
        
        // Ẩn thông tin nhạy cảm cho hồ sơ công khai
        User currentUser = getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(id)) {
            response = new UserProfileResponse(
                response.id(),
                response.username(),
                null, // Hide email
                response.roleName(),
                response.fullName(),
                response.schoolName(),
                response.levelId(),
                response.levelName(),
                response.dob(),
                response.avatar(),
                response.createdAt(),
                response.updatedAt(),
                null, // Hide balance
                response.streak()
            );
        }

        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy hồ sơ người dùng thành công!", response);
    }

    @Override
    public ApiResponse<UserProfileResponse> updateMyInformation(UserInformationRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để cập nhật thông tin!");
        }

        if (request == null || !hasAnyValue(request)) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Vui lòng cung cấp ít nhất một thông tin để cập nhật!");
        }

        UserInformation userInformation = userInformationRepository.findByUser_Id(currentUser.getId()).orElseGet(() -> {
            UserInformation created = new UserInformation();
            created.setUser(currentUser);
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });

        String fullName = normalize(request.fullName());
        if (fullName != null) {
            userInformation.setFullName(fullName);
        }

        String schoolName = normalize(request.schoolName());
        if (schoolName != null) {
            userInformation.setSchoolName(schoolName);
        }

        if (request.levelId() != null) {
            Level level = levelRepository.findById(request.levelId()).orElse(null);
            if (level == null) {
                return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
            }
            userInformation.setLevel(level);
        }

        if (request.dob() != null) {
            userInformation.setDob(request.dob());
        }

        String avatar = normalize(request.avatar());
        if (avatar != null) {
            userInformation.setAvatar(avatar);
        }

        userInformation.setUser(currentUser);
        userInformation.setUpdatedAt(LocalDateTime.now());
        userInformation = userInformationRepository.save(userInformation);

        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(currentUser.getId()).orElse(null);
        return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật thông tin người dùng thành công!", toUserProfileResponse(currentUser, userInformation, streak));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserStreakResponse> getMyStreak() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để xem giữ lửa ôn thi!");
        }

        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(currentUser.getId()).orElse(null);
        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy thông tin giữ lửa ôn thi thành công!", toStreakResponse(streak));
    }

    @Override
    public ApiResponse<UserStreakResponse> checkInMyStreak() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để giữ lửa ôn thi!");
        }

        recordStudyActivity(currentUser.getId(), LocalDate.now());
        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(currentUser.getId()).orElse(null);
        return new ApiResponse<>(HttpStatus.OK.value(), "Giữ lửa ôn thi thành công!", toStreakResponse(streak));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStudyActivity(Integer userId, LocalDate activityDate) {
        if (userId == null || activityDate == null) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(userId).orElseGet(() -> {
            UserStudyStreak created = new UserStudyStreak();
            created.setUser(user);
            created.setCurrentStreak(0);
            created.setLongestStreak(0);
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });

        LocalDate lastActiveDate = streak.getLastActiveDate();
        if (lastActiveDate != null && activityDate.isBefore(lastActiveDate)) {
            return;
        }

        if (lastActiveDate == null) {
            streak.setCurrentStreak(1);
        } else if (activityDate.equals(lastActiveDate)) {
            if (streak.getCurrentStreak() == null || streak.getCurrentStreak() < 1) {
                streak.setCurrentStreak(1);
            }
        } else if (activityDate.equals(lastActiveDate.plusDays(1))) {
            streak.setCurrentStreak(safeInt(streak.getCurrentStreak()) + 1);
        } else {
            streak.setCurrentStreak(1);
        }

        if (streak.getLongestStreak() == null || streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setUser(user);
        streak.setLastActiveDate(activityDate);
        streak.setUpdatedAt(LocalDateTime.now());
        userStudyStreakRepository.save(streak);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<java.util.List<UserProfileResponse>> searchUsers(String query) {
        java.util.List<User> users = userRepository.searchUsers(query);
        java.util.List<UserProfileResponse> responses = users.stream()
                .map(user -> {
                    UserInformation info = userInformationRepository.findByUser_Id(user.getId()).orElse(null);
                    UserStudyStreak streak = userStudyStreakRepository.findByUser_Id(user.getId()).orElse(null);
                    return toUserProfileResponse(user, info, streak);
                })
                .collect(java.util.stream.Collectors.toList());
        
        return new ApiResponse<>(HttpStatus.OK.value(), "Tìm kiếm người dùng thành công!", responses);
    }

    private UserProfileResponse toUserProfileResponse(User user, UserInformation userInformation, UserStudyStreak streak) {
        UserStreakResponse streakResponse = toStreakResponse(streak);
        String roleName = user.getRole() == null ? null : user.getRole().getName();
        Integer levelId = userInformation != null && userInformation.getLevel() != null ? userInformation.getLevel().getId() : null;
        String levelName = userInformation != null && userInformation.getLevel() != null ? userInformation.getLevel().getName() : null;

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleName,
                userInformation != null ? userInformation.getFullName() : null,
                userInformation != null ? userInformation.getSchoolName() : null,
                levelId,
                levelName,
                userInformation != null ? userInformation.getDob() : null,
                userInformation != null ? userInformation.getAvatar() : null,
                user.getCreatedAt(),
                userInformation != null ? userInformation.getUpdatedAt() : null,
                walletRepository.findByUserId(user.getId()).map(Wallet::getBalance).orElse(java.math.BigDecimal.ZERO),
                streakResponse
        );
    }

    private UserStreakResponse toStreakResponse(UserStudyStreak streak) {
        int currentStreak = streak == null || streak.getCurrentStreak() == null ? 0 : streak.getCurrentStreak();
        int longestStreak = streak == null || streak.getLongestStreak() == null ? 0 : streak.getLongestStreak();
        LocalDate lastActiveDate = streak == null ? null : streak.getLastActiveDate();
        
        // Logic: Nếu ngày hoạt động cuối cùng TRƯỚC ngày hôm qua, thì chuỗi bị coi là về 0
        if (lastActiveDate != null && lastActiveDate.isBefore(LocalDate.now().minusDays(1))) {
            currentStreak = 0;
        }

        boolean activeToday = lastActiveDate != null && lastActiveDate.equals(LocalDate.now());
        int fireLevel = calculateFireLevel(currentStreak);
        return new UserStreakResponse(currentStreak, longestStreak, lastActiveDate, activeToday, fireLevel);
    }

    private boolean hasAnyValue(UserInformationRequest request) {
        return normalize(request.fullName()) != null
                || normalize(request.schoolName()) != null
                || request.levelId() != null
                || request.dob() != null
                || normalize(request.avatar()) != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int calculateFireLevel(int currentStreak) {
        if (currentStreak <= 0) {
            return 0;
        }
        if (currentStreak <= 2) {
            return 1;
        }
        if (currentStreak <= 6) {
            return 2;
        }
        if (currentStreak <= 13) {
            return 3;
        }
        if (currentStreak <= 29) {
            return 4;
        }
        return 5;
    }

    @Override
    public java.util.Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public java.util.Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }

        String username = authentication.getName();
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }
}

