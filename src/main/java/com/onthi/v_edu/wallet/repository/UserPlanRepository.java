package com.onthi.v_edu.wallet.repository;

import com.onthi.v_edu.wallet.entity.UserPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlan, Integer> {
    
    @Query("SELECT up FROM UserPlan up WHERE up.user.id = :userId AND up.status = 'ACTIVE' AND up.endDate > CURRENT_TIMESTAMP ORDER BY up.endDate DESC LIMIT 1")
    Optional<UserPlan> findActivePlanByUserId(@Param("userId") Integer userId);
}
