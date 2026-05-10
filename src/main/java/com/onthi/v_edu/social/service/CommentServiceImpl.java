package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.Comment;
import com.onthi.v_edu.social.entity.Post;
import com.onthi.v_edu.social.repository.CommentRepository;
import com.onthi.v_edu.social.repository.PostRepository;
import com.onthi.v_edu.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final com.onthi.v_edu.user.repository.UserInformationRepository userInformationRepository;

    private String getDisplayName(User user) {
        return userInformationRepository.findById(user.getId())
                .map(com.onthi.v_edu.user.entity.UserInformation::getFullName)
                .filter(name -> name != null && !name.isEmpty())
                .orElse(user.getUsername());
    }

    @Override
    @Transactional
    public Comment addComment(User user, Integer postId, Integer parentId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParent(parent);
            
            // Notify parent comment owner
            if (!parent.getUser().getId().equals(user.getId())) {
                notificationService.createNotification(
                    parent.getUser(),
                    "Phản hồi mới",
                    getDisplayName(user) + " đã trả lời bình luận của bạn",
                    "COMMENT_REPLY",
                    "/social"
                );
            }
        }
        
        Comment savedComment = commentRepository.save(comment);

        // Notify post owner
        if (!post.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                post.getUser(),
                "Bình luận mới",
                getDisplayName(user) + " đã bình luận về bài viết của bạn",
                "POST_COMMENT",
                "/social"
            );
        }

        return savedComment;
    }

    @Override
    public List<Comment> getCommentsByPost(Integer postId) {
        List<Comment> comments = commentRepository.findByPost_IdOrderByCreatedAtAsc(postId);
        comments.forEach(comment -> {
            User u = comment.getUser();
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
        return comments;
    }

    @Override
    @Transactional
    public Comment updateComment(User user, Integer commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to update this comment");
        }

        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(User user, Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }
        
        commentRepository.delete(comment);
    }
}
