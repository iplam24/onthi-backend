package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.repository.CommentRepository;
import com.onthi.v_edu.common.fileupload.service.FileUpLoadService;
import com.onthi.v_edu.social.entity.Post;
import com.onthi.v_edu.social.repository.PostLikeRepository;
import com.onthi.v_edu.social.repository.PostRepository;
import com.onthi.v_edu.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserActivityService userActivityService;
    private final NotificationService notificationService;
    private final FollowService followService;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final FileUpLoadService fileUpLoadService;
    private final com.onthi.v_edu.user.repository.UserInformationRepository userInformationRepository;

    private String getDisplayName(User user) {
        return userInformationRepository.findById(user.getId())
                .map(com.onthi.v_edu.user.entity.UserInformation::getFullName)
                .filter(name -> name != null && !name.isEmpty())
                .orElse(user.getUsername());
    }


    @Override
    @Transactional
    public Post createPost(User user, String title, String content, java.util.List<String> images) {
        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setImages(images != null ? images : new java.util.ArrayList<>());
        post.setCreatedAt(LocalDateTime.now());
        Post savedPost = postRepository.save(post);

        // Record as activity
        userActivityService.recordActivity(user, "POST_CREATED", "đã đăng một bài viết mới: " + title, savedPost.getId());

        // Notify followers
        Page<User> followers = followService.getFollowers(user.getId(), Pageable.unpaged());
        followers.forEach(follower -> {
            notificationService.createNotification(
                follower,
                "Bài viết mới",
                getDisplayName(user) + " vừa đăng một bài viết mới: " + title,
                "NEW_POST",
                "/social"
            );
        });

        return savedPost;
    }

    @Override
    public Page<Post> getPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        populateUserNames(posts);
        return posts;
    }

    @Override
    public Page<Post> getFeed(User user, Pageable pageable) {
        java.util.List<Integer> friendIds = followService.getFriendIds(user.getId());
        friendIds.add(user.getId()); // Include self
        Page<Post> posts = postRepository.findByUser_IdInOrderByCreatedAtDesc(friendIds, pageable);
        populateUserNames(posts);
        return posts;
    }

    @Override
    public Page<Post> getUserPosts(Integer userId, Integer requesterId, Pageable pageable) {
        if (userId.equals(requesterId) || followService.isFriend(userId, requesterId)) {
            Page<Post> posts = postRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
            populateUserNames(posts);
            return posts;
        }
        return Page.empty();
    }

    private void populateUserNames(Page<Post> posts) {
        posts.forEach(post -> {
            User u = post.getUser();
            if (u != null) {
                userInformationRepository.findById(u.getId()).ifPresent(info -> {
                    u.setFullName(info.getFullName());
                    u.setAvatar(info.getAvatar());
                });
                if (u.getFullName() == null || u.getFullName().isEmpty()) {
                    u.setFullName(u.getUsername());
                }
            }
        });
    }

    @Override
    @Transactional
    public void deletePost(User user, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (!post.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this post");
        }

        // 1. Delete physical image files
        if (post.getImages() != null) {
            for (String imageUrl : post.getImages()) {
                fileUpLoadService.deleteFile(imageUrl);
            }
        }

        // 2. Delete all likes for this post
        postLikeRepository.deleteByPost_Id(postId);

        // 3. Delete all comments for this post
        commentRepository.deleteByPost_Id(postId);
        
        // 4. Finally delete the post itself
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void likePost(User user, Integer postId) {
        if (postLikeRepository.existsByPost_IdAndUser_Id(postId, user.getId())) {
            return;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        com.onthi.v_edu.social.entity.PostLike like = new com.onthi.v_edu.social.entity.PostLike();
        like.setPost(post);
        like.setUser(user);
        like.setCreatedAt(java.time.LocalDateTime.now());
        postLikeRepository.save(like);

        // Notify post owner
        if (!post.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                post.getUser(),
                "Lượt thích mới",
                getDisplayName(user) + " đã thích bài viết của bạn",
                "POST_LIKE",
                "/social"
            );
        }
    }

    @Override
    @Transactional
    public void unlikePost(User user, Integer postId) {
        postLikeRepository.findByPost_IdAndUser_Id(postId, user.getId())
                .ifPresent(postLikeRepository::delete);
    }

    @Override
    public boolean isLikedByUser(Integer postId, Integer userId) {
        if (userId == null) return false;
        return postLikeRepository.existsByPost_IdAndUser_Id(postId, userId);
    }

    @Override
    public long getLikeCount(Integer postId) {
        return postLikeRepository.countByPost_Id(postId);
    }
}
