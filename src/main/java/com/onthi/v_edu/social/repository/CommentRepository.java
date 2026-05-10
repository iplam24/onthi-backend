package com.onthi.v_edu.social.repository;

import com.onthi.v_edu.social.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByPost_IdOrderByCreatedAtAsc(Integer postId);
    List<Comment> findByParent_IdOrderByCreatedAtAsc(Integer parentId);
    void deleteByPost_Id(Integer postId);
}
