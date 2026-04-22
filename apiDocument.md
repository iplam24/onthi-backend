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

*   **Success Response (200 OK):**

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

*   **Success Response (200 OK):**

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
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách đề thi thành công!",
      "data": [
        { "id": 1, "name": "Đề thi thử THPT Quốc Gia 2024", "subjectId": 1 },
        { "id": 2, "name": "Đề thi giữa kỳ II - Toán 12", "subjectId": 1 }
      ]
    }
    ```

### 3.2 Create Exam

*   **Method:** `POST`
*   **Path:** `/api/exams`
*   **Request Body:**

    ```json
    {
      "name": "Đề thi cuối kỳ I - Lý 12",
      "subjectId": 2,
      "duration": 90
    }
    ```

*   **Success Response (200 OK):**

    ```json
    {
      "status": 201,
      "message": "Tạo đề thi thành công!",
      "data": {
        "id": 3,
        "name": "Đề thi cuối kỳ I - Lý 12",
        "subjectId": 2,
        "duration": 90
      }
    }
    ```

---

## 4. Learning APIs

**Controller:** `LearningController.java`

### 4.1 Get All Subjects

*   **Method:** `GET`
*   **Path:** `/api/learning/subjects`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách môn học thành công!",
      "data": [
        { "id": 1, "name": "Toán" },
        { "id": 2, "name": "Vật Lý" }
      ]
    }
    ```

### 4.2 Create Subject

*   **Method:** `POST`
*   **Path:** `/api/learning/subjects`
*   **Request Body:**

    ```json
    {
      "name": "Hóa Học"
    }
    ```

*   **Success Response (201 Created):**

    ```json
    {
      "status": 201,
      "message": "Tạo môn học thành công!",
      "data": {
        "id": 3,
        "name": "Hóa Học"
      }
    }
    ```

---

## 5. Question APIs

**Controller:** `QuestionController.java`

### 5.1 Get All Questions

*   **Method:** `GET`
*   **Path:** `/api/questions`
*   **Success Response (200 OK):**

    ```json
    {
      "status": 200,
      "message": "Lấy danh sách câu hỏi thành công!",
      "data": [
        { "id": 1, "content": "Câu hỏi 1?", "topicId": 1 },
        { "id": 2, "content": "Câu hỏi 2?", "topicId": 2 }
      ]
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
