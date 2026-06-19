package com.onthi.v_edu.common.setting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    List<SystemSetting> findByCategory(String category);
}
