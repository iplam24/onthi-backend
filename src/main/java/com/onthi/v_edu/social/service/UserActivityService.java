package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.ActivityType;
import com.onthi.v_edu.social.entity.Follow;
import com.onthi.v_edu.social.entity.UserActivity;
import com.onthi.v_edu.social.repository.FollowRepository;
import com.onthi.v_edu.social.repository.UserActivityRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordActivity(User user, String type, String content, Integer targetId) {
        if (user == null) return;

        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setType(ActivityType.valueOf(type));
        activity.setTargetId(targetId);
        activity.setContent(content);
        activity.setCreatedAt(LocalDateTime.now());
        userActivityRepository.save(activity);
    }

    @Transactional
    public void recordActivity(Integer userId, ActivityType type, Integer targetId, String content) {
        User user = userRepository.findById(userId).orElse(null);
        recordActivity(user, type.name(), content, targetId);
    }

    public Page<UserActivity> getFeed(Integer userId, Pageable pageable) {
        // Lấy danh sách ID những người đang theo dõi
        List<Integer> followingIds = followRepository.findByFollower_Id(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(f -> f.getFollowing().getId())
                .collect(Collectors.toList());
        
        // Thêm chính mình vào feed
        followingIds.add(userId);

        return userActivityRepository.findByUserIds(followingIds, pageable);
    }
}
