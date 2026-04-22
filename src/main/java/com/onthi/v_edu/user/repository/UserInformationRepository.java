package com.onthi.v_edu.user.repository;

import com.onthi.v_edu.user.entity.UserInformation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInformationRepository extends JpaRepository<UserInformation, Integer> {
    long countByLevel_Id(Integer levelId);
}

