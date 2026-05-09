-- Dữ liệu mẫu cho các gói cước Onthi-V-Edu

INSERT INTO plans (name, price, duration_days, max_ai_questions_per_day, max_ai_exams_per_month, has_ai_chatbot, has_ai_grading, has_advanced_stats, is_mentor_plan)
VALUES 
('FREE', 0, 9999, 3, 0, false, false, false, false),
('PRO', 49000, 30, 9999, 10, false, false, true, false),
('PREMIUM', 99000, 30, 9999, 9999, true, false, true, false),
('MENTOR', 199000, 30, 9999, 9999, true, true, true, true);
