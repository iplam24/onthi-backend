package com.onthi.v_edu.social.service;

import com.onthi.v_edu.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowService {
    void follow(Integer followerId, Integer followingId);
    void unfollow(Integer followerId, Integer followingId);
    void removeFollower(Integer followingId, Integer followerId);
    void removeFriend(Integer userId, Integer friendId);
    boolean isFollowing(Integer followerId, Integer followingId);
    Page<User> getFollowers(Integer userId, Pageable pageable);
    Page<User> getFollowing(Integer userId, Pageable pageable);
    long getFollowerCount(Integer userId);
    long getFollowingCount(Integer userId);
    boolean isFriend(Integer user1Id, Integer user2Id);
    java.util.List<Integer> getFriendIds(Integer userId);
    Page<User> getFriends(Integer userId, Pageable pageable);
    long getFriendCount(Integer userId);
}
