package com.onthi.v_edu.social.service;

import com.onthi.v_edu.social.entity.Comment;
import com.onthi.v_edu.user.entity.User;
import java.util.List;

public interface CommentService {
    Comment addComment(User user, Integer postId, Integer parentId, String content);
    List<Comment> getCommentsByPost(Integer postId);
    Comment updateComment(User user, Integer commentId, String content);
    void deleteComment(User user, Integer commentId);
}
