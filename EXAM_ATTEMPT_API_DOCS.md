# Tài Liệu API Chi Tiết: Đề Thi, Làm Bài và Chấm Điểm (Exams & Attempts)

Tài liệu này cung cấp chi tiết các API cần thiết để xây dựng luồng thi trắc nghiệm/tự luận ở phía Client.

---

## MỤC LỤC & LUỒNG HOẠT ĐỘNG (WORKFLOW)

Để client tích hợp đúng, vui lòng theo sát luồng sau:
1. **Lấy danh sách đề thi:** Client gọi `GET /api/exams` để lấy danh sách đề.
2. **Xem chi tiết đề thi:** Client gọi `GET /api/exams/{id}` để lấy chi tiết đề thi, **bao gồm danh sách câu hỏi, URL ảnh và các lựa chọn đáp án (options)**.
3. **Bắt đầu làm bài:** Client gọi `POST /api/attempts/start` với `examId` để khởi tạo lượt làm bài. Nhận về `attemptId`. Lưu `attemptId` này lại (ví dụ vào LocalStorage).
4. **Trong khi làm bài:** Nếu phát hiện user chuyển tab hoặc copy/paste, gọi `POST /api/attempts/{attemptId}/violations`.
5. **Nộp bài (Chấm điểm):** Client thu thập mảng câu trả lời và gọi `POST /api/attempts/{attemptId}/submit`. Hệ thống sẽ chấm điểm và trả về kết quả ngay lập tức.
6. **Xem lịch sử:** Client gọi `GET /api/attempts/me` (có phân trang) để xem lịch sử làm bài.

---

## 1. EXAMS API (API ĐỀ THI)

### 1.1. Lấy danh sách đề thi (Có phân trang)
* **Method:** `GET`
* **Path:** `/api/exams` (hoặc `/api/exams/subjects/{subjectId}` để lọc theo môn)
* **Params:** `page` (default 0), `size` (default 10), `sort` (vd: `id,desc`)
* **Response (200 OK):** Trả về `PageResponse` chứa danh sách tổng quan các đề thi.

### 1.2. Lấy chi tiết đề thi (Để làm bài)
* **Method:** `GET`
* **Path:** `/api/exams/{id}`
* **Mô tả:** Trả về toàn bộ cấu hình đề thi cùng với danh sách câu hỏi và các lựa chọn (options). **Client dùng API này để render giao diện thi.**
* **Response mẫu (200 OK):**
```json
{
  "status": 200,
  "message": "Lấy đề thi thành công!",
  "data": {
    "id": 2,
    "title": "Đề thi thử THPT",
    "duration": 90,
    "totalScore": 5.0,
    "questions": [
      {
        "questionId": 5,
        "questionContent": "Khác biệt giữa @Controller và @RestController là gì?",
        "url": "https://cdn.domain.com/image.png", // Có thể null
        "orderIndex": 1,
        "score": 1.0,
        "options": [
          { "id": 21, "content": "Đáp án A" },
          { "id": 22, "content": "Đáp án B" }
        ]
      }
    ]
  }
}
```

---

## 2. ATTEMPTS API (API LÀM BÀI & CHẤM ĐIỂM)

### 2.1. Bắt đầu lượt làm bài
* **Method:** `POST`
* **Path:** `/api/attempts/start`
* **Request Body:**
```json
{
  "examId": 2
}
```
* **Lỗi thường gặp (400 Bad Request):** "Bạn đang có một lượt làm bài chưa nộp cho đề thi này!" -> *Gợi ý cho Client: Nếu gặp lỗi này, hãy lấy `attemptId` cũ đang dang dở để cho user làm tiếp.*
* **Response mẫu (201 Created):**
```json
{
  "status": 201,
  "message": "Bắt đầu làm bài thành công!",
  "data": {
    "id": 29,         // <--- LƯU ID NÀY LẠI ĐỂ SUBMIT
    "examId": 2,
    "status": "DOING",
    "startedAt": "2026-05-05T10:00:00"
  }
}
```

### 2.2. Ghi nhận gian lận (Realtime)
* **Method:** `POST`
* **Path:** `/api/attempts/{attemptId}/violations`
* **Request Body:**
```json
{
  "type": "TAB_SWITCH" // Hoặc "COPY_PASTE"
}
```
* **Response:** Trả về thông tin Attempt hiện tại đã được cộng `tabSwitchCount`.

### 2.3. Nộp bài (Tính điểm)
* **Method:** `POST`
* **Path:** `/api/attempts/{attemptId}/submit`
* **Mô tả:** Gửi danh sách các lựa chọn của user lên để hệ thống chấm điểm. Những câu nào không chọn thì không cần cho vào mảng `answers`.
* **Request Body:**
```json
{
  "answers": [
    {
      "questionId": 5,
      "selectedOptionId": 21,
      "essayAnswer": null
    },
    {
      "questionId": 4,
      "selectedOptionId": null,
      "essayAnswer": "Đây là câu trả lời tự luận."
    }
  ],
  "tabSwitchCount": 2, // Lấy từ state phía client đếm được
  "violationScore": 0
}
```
* **Response mẫu (200 OK) - Trả về Kết Quả:**
```json
{
  "status": 200,
  "message": "Nộp bài thành công!",
  "data": {
    "id": 29,
    "status": "SUBMITTED",
    "score": 4.0,              // Tổng điểm đạt được
    "correctCount": 4,         // Số câu đúng
    "wrongCount": 1,           // Số câu sai
    "totalQuestions": 5,       // Tổng số câu hỏi
    "durationTaken": 1250,     // Thời gian làm bài (giây)
    "tabSwitchCount": 2,       // Số lần chuyển tab
    "answers": [               // Danh sách chấm điểm từng câu
      {
        "questionId": 5,
        "selectedOptionId": 21,
        "isCorrect": true,     // ĐÚNG/SAI
        "score": 1.0           // Điểm của câu này
      }
    ]
  }
}
```

### 2.4. Lịch sử làm bài (Có phân trang)
* **Method:** `GET`
* **Path:** `/api/attempts/me`
* **Params:** `page`, `size`, `sort` (vd: `?page=0&size=10&sort=startedAt,desc`)
* **Response (200 OK):** Trả về `PageResponse` chứa danh sách các lượt làm bài của user.

### 2.5. Lấy chi tiết một lượt làm bài đã qua
* **Method:** `GET`
* **Path:** `/api/attempts/{attemptId}`
* **Mô tả:** Dành cho việc review lại bài đã nộp. Trả về cấu trúc tương tự response của phần Submit (bao gồm `score`, `correctCount`, `answers` đã chọn).
