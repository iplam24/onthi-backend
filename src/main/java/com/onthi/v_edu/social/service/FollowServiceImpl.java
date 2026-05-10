package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.Follow;
import com.onthi.v_edu.social.repository.FollowRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final com.onthi.v_edu.user.repository.UserInformationRepository userInformationRepository;
    private final NotificationService notificationService;

    @Override
    public void follow(Integer followerId, Integer followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        if (followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId)) {
            return; // Already following
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        // Notify
        boolean isMutual = isFollowing(followingId, followerId);
        String fullName = userInformationRepository.findById(follower.getId())
                .map(com.onthi.v_edu.user.entity.UserInformation::getFullName)
                .filter(name -> name != null && !name.isEmpty())
                .orElse(follower.getUsername());
        
        String message = isMutual 
            ? fullName + " đã theo dõi lại bạn. Hai bạn đã trở thành bạn bè!"
            : fullName + " đã bắt đầu theo dõi bạn.";
        
        notificationService.createNotification(
            following,
            isMutual ? "Bạn mới!" : "Người theo dõi mới",
            message,
            "FOLLOW",
            "/profile/" + follower.getId()
        );
    }

    @Override
    public void unfollow(Integer followerId, Integer followingId) {
        followRepository.findByFollower_IdAndFollowing_Id(followerId, followingId)
                .ifPresent(followRepository::delete);
    }

    @Override
    public void removeFollower(Integer followingId, Integer followerId) {
        Follow follow = followRepository.findByFollower_IdAndFollowing_Id(followerId, followingId)
                .orElseThrow(() -> new RuntimeException("This user is not following you"));
        followRepository.delete(follow);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void removeFriend(Integer userId, Integer friendId) {
        // Remove my follow to them
        followRepository.findByFollower_IdAndFollowing_Id(userId, friendId)
                .ifPresent(followRepository::delete);
        // Remove their follow to me
        followRepository.findByFollower_IdAndFollowing_Id(friendId, userId)
                .ifPresent(followRepository::delete);
    }

    @Override
    public boolean isFollowing(Integer followerId, Integer followingId) {
        return followRepository.existsByFollower_IdAndFollowing_Id(followerId, followingId);
    }

    @Override
    public Page<User> getFollowers(Integer userId, Pageable pageable) {
        Page<User> followers = followRepository.findByFollowing_Id(userId, pageable)
                .map(Follow::getFollower);
        return hydrateUsers(followers);
    }

    @Override
    public Page<User> getFollowing(Integer userId, Pageable pageable) {
        Page<User> following = followRepository.findByFollower_Id(userId, pageable)
                .map(Follow::getFollowing);
        return hydrateUsers(following);
    }

    private Page<User> hydrateUsers(Page<User> users) {
        users.forEach(u -> {
            userInformationRepository.findById(u.getId()).ifPresent(info -> {
                u.setFullName(info.getFullName());
                u.setAvatar(info.getAvatar());
            });
            if (u.getFullName() == null || u.getFullName().isEmpty()) {
                u.setFullName(u.getUsername());
            }
        });
        return users;
    }

    @Override
    public long getFollowerCount(Integer userId) {
        return followRepository.countByFollowing_Id(userId);
    }

    @Override
    public long getFollowingCount(Integer userId) {
        return followRepository.countByFollower_Id(userId);
    }

    @Override
    public boolean isFriend(Integer user1Id, Integer user2Id) {
        return isFollowing(user1Id, user2Id) && isFollowing(user2Id, user1Id);
    }

    @Override
    public java.util.List<Integer> getFriendIds(Integer userId) {
        return followRepository.findMutualFollowIds(userId);
    }

    @Override
    public Page<User> getFriends(Integer userId, Pageable pageable) {
        java.util.List<Integer> friendIds = getFriendIds(userId);
        System.out.println("DEBUG: Friend IDs for user " + userId + ": " + friendIds);
        
        if (friendIds == null || friendIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        Page<User> friends = userRepository.findByIdIn(friendIds, pageable);
        System.out.println("DEBUG: Found " + friends.getTotalElements() + " friend users in DB");
        
        return hydrateUsers(friends);
    }

    @Override
    public long getFriendCount(Integer userId) {
        return getFriendIds(userId).size();
    }
}
