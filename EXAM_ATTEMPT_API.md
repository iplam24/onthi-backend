# Exam & Attempt API

Tài liệu riêng cho luồng **ôn thi / làm đề**.

## Scope
- `ExamController`
- `AttemptController`

## Security
- Hầu hết API yêu cầu JWT: `Authorization: Bearer <token>`
- API ghi dữ liệu đề thi yêu cầu `ADMIN`
- API làm bài và xem lịch sử bài làm yêu cầu đăng nhập

---

## 1. Exam APIs

### 1.1 Get all exams

- **Method:** `GET`
- **Path:** `/api/exams`
- **Query params:**
  - `page` mặc định `0`
  - `size` mặc định `10`
  - `sort` mặc định `id,desc`

**Response**
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
        "createdById": 1,
        "createdByUsername": "admin",
        "duration": 90,
        "isActive": true,
        "startTime": null,
        "endTime": null,
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
            "questionContent": "Câu 1?",
            "orderIndex": 1,
            "score": 1.0,
            "contentSnapshot": "Câu 1?"
          }
        ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "numberOfElements": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

### 1.2 Get exams by subject

- **Method:** `GET`
- **Path:** `/api/exams/subjects/{subjectId}`
- **Query params:** `page`, `size`, `sort`

### 1.3 Get exam by id

- **Method:** `GET`
- **Path:** `/api/exams/{id}`

**Response `questions[]` fields**
- `questionId`
- `questionContent`
- `orderIndex`
- `score`
- `contentSnapshot`

### 1.4 Create exam

- **Method:** `POST`
- **Path:** `/api/exams`
- **Role:** `ADMIN`

**Request body**
```json
{
  "title": "Đề thi cuối kỳ I - Lý 12",
  "subjectId": 2,
  "duration": 90,
  "isActive": true,
  "startTime": null,
  "endTime": null,
  "totalScore": 10,
  "type": "MULTIPLE_CHOICE",
  "shuffleQuestions": false,
  "shuffleAnswers": false,
  "maxAttempts": 1,
  "questions": [
    {
      "questionId": 101,
      "orderIndex": 1,
      "score": 1,
      "contentSnapshot": "Câu 1?"
    }
  ]
}
```

### 1.5 Update exam

- **Method:** `PUT`
- **Path:** `/api/exams/{id}`
- **Role:** `ADMIN`

### 1.6 Delete exam

- **Method:** `DELETE`
- **Path:** `/api/exams/{id}`
- **Role:** `ADMIN`

---

## 2. Attempt APIs

### 2.1 Start attempt

- **Method:** `POST`
- **Path:** `/api/attempts/start`

**Request body**
```json
{
  "examId": 1
}
```

**Response**
```json
{
  "status": 201,
  "message": "Bắt đầu làm bài thành công!",
  "data": {
    "id": 123,
    "examId": 1,
    "examTitle": "Đề thi thử THPT Quốc Gia 2024",
    "status": "DOING",
    "score": 0.0,
    "correctCount": 0,
    "wrongCount": 0,
    "totalQuestions": 20,
    "durationTaken": 0,
    "startedAt": "2026-04-22T10:10:00",
    "submittedAt": null,
    "expiredAt": null,
    "tabSwitchCount": 0,
    "violationScore": 0,
    "flagged": false,
    "answers": []
  }
}
```

> Lưu ý: API `start` hiện **không trả danh sách câu hỏi**. Muốn lấy nội dung đề, dùng `GET /api/exams/{id}`.

### 2.2 Submit attempt

- **Method:** `POST`
- **Path:** `/api/attempts/{attemptId}/submit`

**Request body**
```json
{
  "tabSwitchCount": 2,
  "violationScore": 20,
  "answers": [
    {
      "questionId": 10,
      "selectedOptionId": 101
    },
    {
      "questionId": 11,
      "essayAnswer": "Day la bai lam tu luan"
    }
  ]
}
```

**Response**
```json
{
  "status": 200,
  "message": "Nộp bài thành công!",
  "data": {
    "id": 123,
    "examId": 1,
    "examTitle": "Đề thi thử THPT Quốc Gia 2024",
    "status": "SUBMITTED",
    "score": 8.5,
    "correctCount": 17,
    "wrongCount": 3,
    "totalQuestions": 20,
    "durationTaken": 45,
    "startedAt": "2026-04-22T10:10:00",
    "submittedAt": "2026-04-22T10:55:00",
    "expiredAt": null,
    "tabSwitchCount": 2,
    "violationScore": 20,
    "flagged": false,
    "answers": [
      {
        "questionId": 10,
        "questionContent": "Câu hỏi 1?",
        "selectedOptionId": 101,
        "essayAnswer": null,
        "isCorrect": true,
        "score": 1.0
      }
    ]
  }
}
```

### 2.3 Get attempt by id

- **Method:** `GET`
- **Path:** `/api/attempts/{attemptId}`

**Response fields**
- `id`
- `examId`
- `examTitle`
- `status`
- `score`
- `correctCount`
- `wrongCount`
- `totalQuestions`
- `durationTaken`
- `startedAt`
- `submittedAt`
- `expiredAt`
- `tabSwitchCount`
- `violationScore`
- `flagged`
- `answers[]`

### 2.4 Get my attempts

- **Method:** `GET`
- **Path:** `/api/attempts/me`

**Response**
```json
{
  "status": 200,
  "message": "Lấy lịch sử làm bài thành công!",
  "data": [
    {
      "id": 123,
      "examId": 1,
      "examTitle": "Đề thi thử THPT Quốc Gia 2024",
      "status": "SUBMITTED",
      "score": 8.5,
      "correctCount": 17,
      "wrongCount": 3,
      "totalQuestions": 20,
      "durationTaken": 45,
      "startedAt": "2026-04-22T10:10:00",
      "submittedAt": "2026-04-22T10:55:00",
      "expiredAt": null,
      "tabSwitchCount": 0,
      "violationScore": 0,
      "flagged": false
    }
  ]
}
```

---

## 3. Anti-cheat rules

- Không cho start nếu đề không active hoặc ngoài khung thời gian
- Không cho start nếu user đã có attempt `DOING` cùng đề
- Áp dụng `maxAttempts`
- Không cho submit nếu attempt đã `SUBMITTED` hoặc `EXPIRED`
- Server tự kiểm tra deadline từ `startedAt + duration` và `exam.endTime`
- Không cho submit question không thuộc đề
- Không cho submit trùng `questionId`
- MCQ phải dùng `selectedOptionId`
- ESSAY phải dùng `essayAnswer`
- Điểm số tính hoàn toàn ở server
- `tabSwitchCount >= 5` hoặc `violationScore >= 50` sẽ bị gắn cờ `flagged = true`

---

## 4. Notes

- `ExamResponse.questions` là danh sách câu hỏi của đề, mỗi item gồm `questionId`, `questionContent`, `orderIndex`, `score`, `contentSnapshot`.
- `QuestionResponse` có thêm `url` để FE gán ảnh minh họa cho câu hỏi.
- Khi làm bài, FE nên lấy danh sách câu hỏi từ `GET /api/exams/{id}` trước, còn `POST /api/attempts/start` chỉ tạo lượt làm bài.
- Khi dùng upload ảnh cho câu hỏi, client có thể lấy `url` từ API upload rồi gán vào `QuestionRequest.url`.

