# Báo cáo các thay đổi tính năng Đề thi & Chấm điểm (Exam & Attempt)

Dưới đây là danh sách chi tiết tất cả những sửa đổi đã được thêm vào hệ thống backend nhằm hỗ trợ UI/UX cho frontend, đặc biệt là các dạng đề thi tự luận/đề văn và tính năng chấm thi bằng AI.

---

## 1. Trả về Feedback (Nhận xét) đầy đủ của AI khi chấm câu tự luận
*Tính năng đã được hỗ trợ thông qua API Nộp bài hoặc Xem lại bài làm.*

- **DTO bị ảnh hưởng:** `AttemptAnswerResponse`
- **Cách hoạt động:**
  - Ở Backend, sau khi AI chấm xong một câu tự luận, kết quả và lời nhận xét sẽ được lưu vào trường `aiFeedback` của entity `Answer`.
  - Trong `AttemptServiceImpl.java` (hàm `toAttemptDetailResponse`), backend tự động map `answer.getAiFeedback()` vào trường `feedback` của `AttemptAnswerResponse`.
  - Khi client gọi API submit hoặc lấy chi tiết attempt, response trả về mảng `answers`. Trong mỗi object answer, frontend có thể lấy trường `feedback` và `gradingMethod` để hiển thị nhận xét.

**Mẫu Response minh hoạ cho FE:**
```json
"answers": [
  {
    "questionId": 10,
    "essayAnswer": "Bài làm văn...",
    "score": 4.5,
    "feedback": "AI Nhận xét: Bài viết có ý tốt, bố cục rõ ràng nhưng thiếu ví dụ.",
    "gradingMethod": "Chấm bằng Gemini AI"
  }
]
```

---

## 2. Nhận diện loại đề thi (như Đề Văn / Tự luận) để Frontend tuỳ chỉnh UI
*Thêm trường cấu hình trực tiếp vào CSDL để cho phép admin kiểm soát.*

- **File thay đổi:** `Exam.java` (Entity), `ExamRequest.java` (DTO), `ExamServiceImpl.java` (Service).
- **Chi tiết cập nhật:**
  - Thêm trường `uiLayoutHint` (kiểu `String`) vào entity `Exam` lưu trực tiếp trong DB.
  - Cập nhật `ExamRequest` để cho phép client (Admin) gửi trường `uiLayoutHint` khi tạo mới hoặc chỉnh sửa đề thi (`POST /api/exams`, `PUT /api/exams/{id}`).
  - Các giá trị gợi ý: `STANDARD` (mặc định), `LITERATURE` (Đề văn nghị luận), `ESSAY` (100% tự luận), `MIXED` (Hỗn hợp).
  - Nếu khi tạo đề admin **không truyền** `uiLayoutHint`, backend (`ExamServiceImpl`) vẫn có logic fallback tự động nhận diện dựa trên tên môn học (chứa từ "văn", "literature") và loại câu hỏi để tự động trả về hint phù hợp.

---

## 3. Tự động chia Phần thi (Sections) với format: Phần 1, Phần 2
*Nhóm và chia cụm các câu hỏi theo loại nhằm hiển thị cấu trúc đề thi chuẩn trên Frontend.*

- **File thay đổi:** `ExamServiceImpl.java`
- **Chi tiết cập nhật:**
  - Viết lại hàm `buildSectionTitle()` để tự động sinh ra tên các phần thi theo chuẩn số đếm: `Phần 1`, `Phần 2`,... thay vì số La Mã (`Phần I`, `Phần II`) như logic cũ.
  - Tên của mỗi section cũng được nối thêm đuôi chú thích thể loại tương ứng, ví dụ:
    - **`Phần 1 - Trắc nghiệm`**
    - **`Phần 2 - Tự luận`**
  - Frontend có thể dựa vào trường `sections` nằm trong `ExamResponse` để render ra các khối giao diện (block) nhóm câu hỏi một cách đẹp mắt.
