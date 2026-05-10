package com.onthi.v_edu.social.repository;

import com.onthi.v_edu.social.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<Post> findByUser_IdInOrderByCreatedAtDesc(java.util.Collection<Integer> userIds, Pageable pageable);
}
