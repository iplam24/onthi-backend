package com.onthi.v_edu.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFixer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixDatabase() {
        try {
            System.out.println("[DB FIXER] Đang kiểm tra và sửa cột status trong bảng attempts...");
            jdbcTemplate.execute("ALTER TABLE attempts MODIFY COLUMN status VARCHAR(50)");
            System.out.println("[DB FIXER] Đã sửa cột status thành công!");
        } catch (Exception e) {
            System.err.println("[DB FIXER] Lỗi khi sửa database: " + e.getMessage());
        }
    }
}
