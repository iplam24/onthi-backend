package com.onthi.v_edu.social.repository;

import com.onthi.v_edu.social.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollower_IdAndFollowing_Id(Integer followerId, Integer followingId);

    Page<Follow> findByFollower_Id(Integer followerId, Pageable pageable);

    Page<Follow> findByFollowing_Id(Integer followingId, Pageable pageable);

    long countByFollower_Id(Integer followerId);

    long countByFollowing_Id(Integer followingId);

    boolean existsByFollower_IdAndFollowing_Id(Integer followerId, Integer followingId);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId AND f.following.id IN (SELECT f2.follower.id FROM Follow f2 WHERE f2.following.id = :userId)")
    List<Integer> findMutualFollowIds(@Param("userId") Integer userId);
}
