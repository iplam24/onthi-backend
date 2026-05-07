-- ===================== CONFIG =====================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===================== ROLE =====================
CREATE TABLE roles (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ===================== LEVEL =====================
CREATE TABLE levels (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) UNIQUE
) ENGINE=InnoDB;

-- ===================== USER =====================
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(100),
                       email VARCHAR(100) UNIQUE,
                       password VARCHAR(255),
                       role_id INT,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE user_information (
                                  user_id INT PRIMARY KEY,
                                  full_name VARCHAR(150),
                                  school_name VARCHAR(255),
                                  level_id INT,
                                  dob DATE,
                                  avatar VARCHAR(255),
                                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                  FOREIGN KEY (level_id) REFERENCES levels(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE user_study_streaks (
                                  user_id INT PRIMARY KEY,
                                  current_streak INT DEFAULT 0,
                                  longest_streak INT DEFAULT 0,
                                  last_active_date DATE,
                                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== WALLET =====================
CREATE TABLE wallets (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         user_id INT UNIQUE,
                         balance DECIMAL(15,2) DEFAULT 0,
                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE transactions (
                              id INT AUTO_INCREMENT PRIMARY KEY,
                              user_id INT,
                              amount DECIMAL(15,2),
                              type ENUM('DEPOSIT','PURCHASE','REFUND','WITHDRAW'),
                              status ENUM('PENDING','SUCCESS','FAILED'),
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE plans (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) UNIQUE,
                       price DECIMAL(15,2),
                       duration_days INT
) ENGINE=InnoDB;

CREATE TABLE user_plans (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT,
                            plan_id INT,
                            status ENUM('ACTIVE','EXPIRED','CANCELLED') DEFAULT 'ACTIVE',
                            start_date DATETIME,
                            end_date DATETIME,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (plan_id) REFERENCES plans(id)
) ENGINE=InnoDB;

-- ===================== LEARNING =====================
CREATE TABLE subjects (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(100),
                          level_id INT,
                          FOREIGN KEY (level_id) REFERENCES levels(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE topics (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100),
                        subject_id INT,
                        FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== QUESTIONS (ADMIN) =====================
CREATE TABLE questions (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           content TEXT,
                           url VARCHAR(500),
                           type ENUM('MCQ','ESSAY'),
                           difficulty ENUM('EASY','MEDIUM','HARD'),
                           topic_id INT,
                           created_by INT,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
                           FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE question_options (
                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                  question_id INT,
                                  content TEXT,
                                  is_correct BOOLEAN,
                                  FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE essay_answers (
                               id INT AUTO_INCREMENT PRIMARY KEY,
                               question_id INT,
                               sample_answer LONGTEXT,
                               FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE explanations (
                              question_id INT PRIMARY KEY,
                              content TEXT,
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== USER QUESTIONS (PRIVATE) =====================
CREATE TABLE user_questions (
                                id INT AUTO_INCREMENT PRIMARY KEY,
                                user_id INT,
                                content TEXT,
                                type ENUM('MCQ','ESSAY'),
                                difficulty ENUM('EASY','MEDIUM','HARD'),
                                topic_id INT,
                                is_public BOOLEAN DEFAULT FALSE,
                                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE user_question_options (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       question_id INT,
                                       content TEXT,
                                       is_correct BOOLEAN,
                                       FOREIGN KEY (question_id) REFERENCES user_questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_essay_answers (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    question_id INT,
                                    sample_answer LONGTEXT,
                                    FOREIGN KEY (question_id) REFERENCES user_questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== IMPORT LOG =====================
CREATE TABLE question_import_logs (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      user_question_id INT,
                                      imported_question_id INT,
                                      imported_by INT,
                                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                      FOREIGN KEY (imported_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- ===================== EXAM =====================
CREATE TABLE exams (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       title VARCHAR(255),
                       subject_id INT,
                       created_by INT,
                       duration INT,
                       FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
                       FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE exam_questions (
                                exam_id INT,
                                question_id INT,
                                PRIMARY KEY (exam_id, question_id),
                                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
                                FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== ATTEMPT (HISTORY CORE) =====================
CREATE TABLE attempts (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT,
                          exam_id INT,
                          status ENUM('DOING','SUBMITTED','EXPIRED') DEFAULT 'DOING',
                          score DECIMAL(5,2),

                          correct_count INT DEFAULT 0,
                          wrong_count INT DEFAULT 0,
                          total_questions INT,
                          duration_taken INT,

                          started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          submitted_at DATETIME,
                          expired_at DATETIME,

                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                          FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== ANSWERS (SNAPSHOT) =====================
CREATE TABLE answers (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         attempt_id INT,
                         question_id INT,

                         selected_option_id INT,
                         essay_answer TEXT,

                         is_correct BOOLEAN,
                         score DECIMAL(5,2) DEFAULT 0,

    -- 🔥 SNAPSHOT
                         question_snapshot TEXT,
                         correct_answer_snapshot TEXT,

                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         UNIQUE (attempt_id, question_id),

                         FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
                         FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
                         FOREIGN KEY (selected_option_id) REFERENCES question_options(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ===================== CLASS =====================
CREATE TABLE classes (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(255),
                         teacher_id INT,
                         FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE class_students (
                                class_id INT,
                                student_id INT,
                                PRIMARY KEY (class_id, student_id),
                                FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
                                FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== ROOM =====================
CREATE TABLE rooms (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255),
                       created_by INT,
                       status ENUM('WAITING','STARTED','ENDED') DEFAULT 'WAITING',
                       FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE room_participants (
                                   room_id INT,
                                   user_id INT,
                                   score DECIMAL(5,2),
                                   PRIMARY KEY (room_id, user_id),
                                   FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
                                   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== SOCIAL =====================
CREATE TABLE posts (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       user_id INT,
                       title VARCHAR(255),
                       content TEXT,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE comments (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          post_id INT,
                          user_id INT,
                          parent_id INT,
                          content TEXT,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                          FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE likes (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       user_id INT,
                       target_id INT,
                       target_type ENUM('POST','COMMENT'),
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       UNIQUE(user_id, target_id, target_type),
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== Q&A =====================
CREATE TABLE qa_questions (
                              id INT AUTO_INCREMENT PRIMARY KEY,
                              student_id INT,
                              teacher_id INT,
                              title VARCHAR(255),
                              content TEXT,
                              status ENUM('OPEN','ANSWERED','CLOSED') DEFAULT 'OPEN',
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                              FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE qa_answers (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            question_id INT,
                            teacher_id INT,
                            content TEXT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (question_id) REFERENCES qa_questions(id) ON DELETE CASCADE,
                            FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE teacher_reviews (
                                 id INT AUTO_INCREMENT PRIMARY KEY,
                                 teacher_id INT,
                                 student_id INT,
                                 rating INT,
                                 comment TEXT,
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
                                 FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== PROGRESS =====================
CREATE TABLE progress (
                          user_id INT,
                          topic_id INT,
                          correct_count INT DEFAULT 0,
                          wrong_count INT DEFAULT 0,
                          PRIMARY KEY(user_id, topic_id),
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                          FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;