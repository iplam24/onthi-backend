# API Documentation

This document provides a comprehensive overview of all the APIs available in the system, including JSON examples for requests and responses.

---

## 1. Authentication APIs

**Controller:** `AuthController.java`

### 1.1 User Login

*   **Method:** `POST`
*   **Path:** `/api/auth/login`
*   **Description:** Authenticates a user and returns a JWT token.
*   **Request Body:**

    ```json
    {
      "username": "admin",
      "password": "admin123"
    }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 200,
      "message": "Đăng nhập thành công!",
      "data": {
        "token": "jwt_token_here",
        "type": "Bearer",
        "id": 1,
        "username": "admin",
        "email": "admin@v-edu.local",
        "roles": ["ROLE_ADMIN"]
      }
    }
    ```

### 1.2 User Registration

*   **Method:** `POST`
*   **Path:** `/api/auth/register`
*   **Description:** Registers a new user.
*   **Request Body:**

    ```json
    {
      "username": "newuser",
      "email": "newuser@example.com",
      "password": "password123"
    }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 200,
      "message": "Đăng ký thành công!",
      "data": null
    }
    ```

---

## 2. Attempt APIs

**Controller:** `AttemptController.java`

### 2.1 Start a New Attempt

*   **Method:** `POST`
*   **Path:** `/api/attempts/start`
*   **Request Body:**

    ```json
    {
      "examId": 1
    }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Bắt đầu làm bài thành công!",
      "data": {
        "attemptId": 123,
        "examId": 1,
        "startTime": "2023-10-27T10:00:00Z",
        "questions": [
          { "id": 1, "content": "Câu hỏi 1?" },
          { "id": 2, "content": "Câu hỏi 2?" }
        ]
      }
    }
    ```

### 2.2 Submit an Attempt

*   **Method:** `POST`
*   **Path:** `/api/attempts/{attemptId}/submit`
*   **Request Body:**

    ```json
    {
      "answers": [
        { "questionId": 1, "answer": "A" },
        { "questionId": 2, "answer": "B" }
      ]
    }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Nộp bài thành công!",
      "data": {
        "attemptId": 123,
        "score": 8.5,
        "totalCorrect": 17,
        "totalIncorrect": 3
      }
    }
    ```

### 2.3 Get Attempt by ID

*   **Method:** `GET`
*   **Path:** `/api/attempts/{attemptId}`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy thông tin bài làm thành công!",
      "data": {
        "attemptId": 123,
        "examId": 1,
        "score": 8.5,
        "startTime": "2023-10-27T10:00:00Z",
        "endTime": "2023-10-27T10:45:00Z"
      }
    }
    ```

### 2.4 Get My Attempts

*   **Method:** `GET`
*   **Path:** `/api/attempts/me`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách bài làm thành công!",
      "data": [
        {
          "attemptId": 123,
          "examId": 1,
          "score": 8.5,
          "startTime": "2023-10-27T10:00:00Z"
        },
        {
          "attemptId": 124,
          "examId": 2,
          "score": 9.0,
          "startTime": "2023-10-28T11:00:00Z"
        }
      ]
    }
    ```

---

## 3. Exam APIs

**Controller:** `ExamController.java`

### 3.1 Get All Exams

*   **Method:** `GET`
*   **Path:** `/api/exams`
*   **Query Parameters:**
    *   `page` - số trang, mặc định `0`
    *   `size` - số phần tử mỗi trang, mặc định `10`
    *   `sort` - tùy chọn, mặc định `id,desc`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách đề thi thành công!",
      "data": {
        "items": [
          {
            "id": 1,
            "title": "Đề thi thử THPT Quốc Gia 2024",
            "subjectId": 1,
            "subjectName": "Toán",
            "createdByUsername": "admin",
            "duration": 90,
            "isActive": true,
            "totalScore": 10.0,
            "type": "MULTIPLE_CHOICE",
            "shuffleQuestions": true,
            "shuffleAnswers": true,
            "maxAttempts": 1,
            "createdAt": "2026-04-22T10:00:00",
            "updatedAt": null,
            "questions": [
              {
                "questionId": 101,
                "content": "Câu 1?",
                "orderIndex": 1,
                "score": 1.0,
                "contentSnapshot": "Câu 1?"
              }
            ]
          },
          {
            "id": 2,
            "title": "Đề thi giữa kỳ II - Toán 12",
            "subjectId": 1,
            "subjectName": "Toán",
            "createdByUsername": "admin",
            "duration": 60,
            "isActive": true,
            "totalScore": 10.0,
            "type": "MIXED",
            "shuffleQuestions": false,
            "shuffleAnswers": false,
            "maxAttempts": 2,
            "createdAt": "2026-04-22T10:10:00",
            "updatedAt": null,
            "questions": [
              {
                "questionId": 201,
                "content": "Câu 2?",
                "orderIndex": 1,
                "score": 1.0,
                "contentSnapshot": "Câu 2?"
              }
            ]
          }
        ],
        "page": 0,
        "size": 10,
        "totalElements": 25,
        "totalPages": 3,
        "numberOfElements": 10,
        "first": true,
        "last": false,
        "hasNext": true,
        "hasPrevious": false
      }
    }
    ```

### 3.2 Get Exams By Subject

*   **Method:** `GET`
*   **Path:** `/api/exams/subjects/{subjectId}`
*   **Query Parameters:**
    *   `page` - số trang, mặc định `0`
    *   `size` - số phần tử mỗi trang, mặc định `10`
    *   `sort` - tùy chọn, mặc định `id,desc`
*   **Success Response (200 OK):** tương tự `Get All Exams`, nhưng chỉ chứa các đề thi của môn học được chỉ định.

### 3.3 Create Exam

*   **Method:** `POST`
*   **Path:** `/api/exams`
*   **Request Body:**

    ```json
    {
      "title": "Đề thi cuối kỳ I - Lý 12",
      "subjectId": 2,
      "duration": 90,
      "isActive": true,
      "type": "MULTIPLE_CHOICE"
    }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 201,
      "message": "Tạo đề thi thành công!",
      "data": {
        "id": 3,
        "title": "Đề thi cuối kỳ I - Lý 12",
        "subjectId": 2,
        "subjectName": "Vật Lý",
        "createdByUsername": "admin",
        "duration": 90,
        "isActive": true,
        "totalScore": 10.0,
        "type": "MULTIPLE_CHOICE",
        "shuffleQuestions": false,
        "shuffleAnswers": false,
        "maxAttempts": 1,
        "createdAt": "2026-04-22T10:20:00",
        "updatedAt": null,
        "questions": [
          {
            "questionId": 101,
            "content": "Câu 1?",
            "orderIndex": 1,
            "score": 1.0,
            "contentSnapshot": "Câu 1?"
          }
        ]
      }
    }
    ```

---

## 4. Learning APIs

**Controller:** `LearningController.java`

### 4.1 Levels API

*   **Method:** `GET`
*   **Path:** `/api/learning/levels`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách level thành công!",
      "data": [
        { "id": 1, "name": "Lớp 10" },
        { "id": 2, "name": "Lớp 11" }
      ]
    }
    ```

*   **Method:** `GET`
*   **Path:** `/api/learning/levels/{id}`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy level thành công!",
      "data": { "id": 1, "name": "Lớp 10" }
    }
    ```

*   **Method:** `POST`
*   **Path:** `/api/learning/levels`
*   **Authorization:** `ADMIN`
*   **Request Body:**

    ```json
    { "name": "Lớp 12" }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 201,
      "message": "Tạo level thành công!",
      "data": { "id": 3, "name": "Lớp 12" }
    }
    ```

*   **Method:** `PUT`
*   **Path:** `/api/learning/levels/{id}`
*   **Authorization:** `ADMIN`
*   **Request Body:**

    ```json
    { "name": "Lớp 12 nâng cao" }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Cập nhật level thành công!",
      "data": { "id": 3, "name": "Lớp 12 nâng cao" }
    }
    ```

*   **Method:** `DELETE`
*   **Path:** `/api/learning/levels/{id}`
*   **Authorization:** `ADMIN`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Xoá level thành công!",
      "data": null
    }
    ```

### 4.2 Subjects API

*   **Method:** `GET`
*   **Path:** `/api/learning/subjects`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách subject thành công!",
      "data": [
        { "id": 1, "name": "Toán", "levelId": 1, "levelName": "Lớp 10" },
        { "id": 2, "name": "Vật Lý", "levelId": 2, "levelName": "Lớp 11" }
      ]
    }
    ```

*   **Method:** `GET`
*   **Path:** `/api/learning/subjects/{id}`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy subject thành công!",
      "data": { "id": 1, "name": "Toán", "levelId": 1, "levelName": "Lớp 10" }
    }
    ```

*   **Method:** `POST`
*   **Path:** `/api/learning/subjects`
*   **Authorization:** `ADMIN`
*   **Request Body:**

    ```json
    { "name": "Hóa Học", "levelId": 2 }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 201,
      "message": "Tạo subject thành công!",
      "data": { "id": 3, "name": "Hóa Học", "levelId": 2, "levelName": "Lớp 11" }
    }
    ```

*   **Method:** `PUT`
*   **Path:** `/api/learning/subjects/{id}`
*   **Authorization:** `ADMIN`
*   **Request Body:**

    ```json
    { "name": "Hóa Học nâng cao", "levelId": 2 }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Cập nhật subject thành công!",
      "data": { "id": 3, "name": "Hóa Học nâng cao", "levelId": 2, "levelName": "Lớp 11" }
    }
    ```

*   **Method:** `DELETE`
*   **Path:** `/api/learning/subjects/{id}`
*   **Authorization:** `ADMIN`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Xoá subject thành công!",
      "data": null
    }
    ```

### 4.3 Topics API

*   **Method:** `GET`
*   **Path:** `/api/learning/topics`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách topic thành công!",
      "data": [
        {
          "id": 1,
          "name": "Hàm số",
          "subjectId": 1,
          "subjectName": "Toán",
          "levelId": 1,
          "levelName": "Lớp 10"
        }
      ]
    }
    ```

*   **Method:** `GET`
*   **Path:** `/api/learning/topics/{id}`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy topic thành công!",
      "data": {
        "id": 1,
        "name": "Hàm số",
        "subjectId": 1,
        "subjectName": "Toán",
        "levelId": 1,
        "levelName": "Lớp 10"
      }
    }
    ```

*   **Method:** `POST`
*   **Path:** `/api/learning/topics`
*   **Authorization:** `ADMIN`
*   **Request Body:**

    ```json
    { "name": "Hàm số bậc hai", "subjectId": 1 }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 201,
      "message": "Tạo topic thành công!",
      "data": {
        "id": 2,
        "name": "Hàm số bậc hai",
        "subjectId": 1,
        "subjectName": "Toán",
        "levelId": 1,
        "levelName": "Lớp 10"
      }
    }
    ```

*   **Method:** `PUT`
*   **Path:** `/api/learning/topics/{id}`
*   **Authorization:** `ADMIN`
*   **Request Body:**

    ```json
    { "name": "Hàm số nâng cao", "subjectId": 1 }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Cập nhật topic thành công!",
      "data": {
        "id": 2,
        "name": "Hàm số nâng cao",
        "subjectId": 1,
        "subjectName": "Toán",
        "levelId": 1,
        "levelName": "Lớp 10"
      }
    }
    ```

*   **Method:** `DELETE`
*   **Path:** `/api/learning/topics/{id}`
*   **Authorization:** `ADMIN`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Xoá topic thành công!",
      "data": null
    }
    ```

### Notes

*   Các API ghi dữ liệu trong `LearningController` yêu cầu quyền `ADMIN`.
*   Các response lỗi thường gặp:
    *   `404 Not Found` khi không tìm thấy `level`, `subject`, hoặc `topic`.
    *   `400 Bad Request` khi tên đã tồn tại hoặc dữ liệu đang được sử dụng nên không thể xoá.

---

## 5. Question APIs

**Controller:** `QuestionController.java`

### 5.1 Get All Questions

*   **Method:** `GET`
*   **Path:** `/api/questions`
*   **Query Parameters:**
    *   `page` - số trang, mặc định `0`
    *   `size` - số phần tử mỗi trang, mặc định `10`
    *   `sort` - tùy chọn, mặc định `id,desc`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách câu hỏi thành công!",
      "data": {
        "items": [
          {
            "id": 1,
            "content": "Câu hỏi 1?",
            "type": "MCQ",
            "difficulty": "EASY",
            "topicId": 1,
            "topicName": "Đại số",
            "createdByUsername": "admin",
            "createdAt": "2026-04-22T10:00:00",
            "options": [
              { "id": 11, "content": "A", "isCorrect": false },
              { "id": 12, "content": "B", "isCorrect": true }
            ],
            "sampleAnswer": null,
            "explanation": "Giải thích ngắn",
            "explanationCreatedAt": "2026-04-22T10:05:00"
          },
          {
            "id": 2,
            "content": "Câu hỏi 2?",
            "type": "ESSAY",
            "difficulty": "MEDIUM",
            "topicId": 2,
            "topicName": "Hình học",
            "createdByUsername": "admin",
            "createdAt": "2026-04-22T10:15:00",
            "options": [],
            "sampleAnswer": "Đáp án mẫu",
            "explanation": null,
            "explanationCreatedAt": null
          }
        ],
        "page": 0,
        "size": 10,
        "totalElements": 42,
        "totalPages": 5,
        "numberOfElements": 10,
        "first": true,
        "last": false,
        "hasNext": true,
        "hasPrevious": false
      }
    }
    ```

### 5.2 Create Question

*   **Method:** `POST`
*   **Path:** `/api/questions`
*   **Request Body:**

    ```json
    {
      "content": "Câu hỏi 3?",
      "topicId": 1,
      "options": [
        { "content": "Đáp án A", "isCorrect": false },
        { "content": "Đáp án B", "isCorrect": true }
      ]
    }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 201,
      "message": "Tạo câu hỏi thành công!",
      "data": {
        "id": 3,
        "content": "Câu hỏi 3?",
        "topicId": 1
      }
    }
    ```

---

## 6. System APIs

### 6.1 Health Check

*   **Method:** `GET`
*   **Path:** `/api/health`
*   **Description:** Checks the health of the application.
*   **Success Response (200 OK):**

    ```
    Application is running!
    ```

### 6.2 Application Information

*   **Method:** `GET`
*   **Path:** `/api/info`
*   **Description:** Returns basic information about the application.
*   **Success Response (200 OK):**

    ```
    Version: 1.0.0
    ```

### 6.3 VV Endpoint

*   **Method:** `GET`
*   **Path:** `/api/vv`
*   **Description:** A placeholder endpoint.
*   **Success Response (200 OK):**

    ```
    VV endpoint is working!
    ```
