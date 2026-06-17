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
            
            System.out.println("[DB FIXER] Đang cập nhật cột type và description trong bảng transactions...");
            jdbcTemplate.execute("ALTER TABLE transactions MODIFY COLUMN type VARCHAR(50)");
            
            try {
                jdbcTemplate.execute("ALTER TABLE transactions ADD COLUMN description VARCHAR(500)");
            } catch (Exception e) {
                // Column might already exist
            }

            // Fix questions.type column to support LISTENING and SPEAKING enum values
            try {
                jdbcTemplate.execute("ALTER TABLE questions MODIFY COLUMN type VARCHAR(50)");
                System.out.println("[DB FIXER] Đã sửa cột type trong bảng questions (LISTENING/SPEAKING)");
            } catch (Exception e) {
                // Column might already be VARCHAR or have the values
                System.out.println("[DB FIXER] Cột type trong questions đã ổn: " + e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE question_options ADD COLUMN image_url VARCHAR(500)");
                System.out.println("[DB FIXER] Đã thêm cột image_url trong bảng question_options");
            } catch (Exception e) {
                System.out.println("[DB FIXER] Cột image_url trong question_options đã ổn: " + e.getMessage());
            }

            System.out.println("[DB FIXER] Đã sửa database thành công!");
        } catch (Exception e) {
            System.err.println("[DB FIXER] Lỗi khi sửa database: " + e.getMessage());
        }
    }
}
