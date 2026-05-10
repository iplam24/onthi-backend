package com.onthi.v_edu.social.repository;

import com.onthi.v_edu.social.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    Optional<PostLike> findByPost_IdAndUser_Id(Integer postId, Integer userId);
    boolean existsByPost_IdAndUser_Id(Integer postId, Integer userId);
    long countByPost_Id(Integer postId);
    void deleteByPost_Id(Integer postId);
}
