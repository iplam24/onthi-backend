package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.Post;
import com.onthi.v_edu.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Post createPost(User user, String title, String content, java.util.List<String> images);
    Page<Post> getPosts(Pageable pageable);
    Page<Post> getFeed(User user, Pageable pageable);
    Page<Post> getUserPosts(Integer userId, Integer requesterId, Pageable pageable);
    void deletePost(User user, Integer postId);
    void likePost(User user, Integer postId);
    void unlikePost(User user, Integer postId);
    boolean isLikedByUser(Integer postId, Integer userId);
    long getLikeCount(Integer postId);
}
