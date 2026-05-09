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
            
            System.out.println("[DB FIXER] Đang cập nhật ENUM cho cột type trong bảng transactions...");
            jdbcTemplate.execute("ALTER TABLE transactions MODIFY COLUMN type ENUM('DEPOSIT','PURCHASE','REFUND','WITHDRAWAL')");
            
            System.out.println("[DB FIXER] Đã sửa database thành công!");
        } catch (Exception e) {
            System.err.println("[DB FIXER] Lỗi khi sửa database: " + e.getMessage());
        }
    }
}
