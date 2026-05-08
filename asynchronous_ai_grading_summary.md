# Hệ Thống Chấm Bài AI Không Đồng Bộ (Asynchronous AI Grading)

Tài liệu này tổng hợp các thay đổi và mô tả luồng hoạt động hiện tại của hệ thống chấm bài sử dụng AI.

## 1. Các Thay Đổi Chính

### Trạng Thái Lượt Làm Bài (Attempt Status)
- Sử dụng trạng thái **`GRADING`** làm trạng thái trung gian khi bài đang được AI xử lý.
- Sau khi hoàn tất, trạng thái sẽ chuyển thành **`SUBMITTED`**.

### Dịch Vụ Chấm Bài (AI Services)
- **GeminiAiGradingService**:
    - Hỗ trợ chấm bài theo lô (Batch) cho toàn bộ đề thi trong một lần gọi API.
    - Xử lý được cả câu hỏi Trắc nghiệm (MCQ) và Tự luận (Essay).
    - Cung cấp feedback chi tiết cho từng câu hỏi.
    - Sử dụng `ObjectMapper` để đảm bảo kết quả JSON được parse chính xác.
- **GitHubModelsAiGradingService**:
    - Được nâng cấp để hỗ trợ chấm bài theo lô tương tự Gemini.
    - Đóng vai trò là phương án dự phòng (Fallback) khi Gemini gặp lỗi hoặc hết hạn mức.

### Xử Lý Không Đồng Bộ (Background Processing)
- **AttemptAsyncGradingService**:
    - Thu thập toàn bộ câu hỏi và câu trả lời của một lượt làm bài.
    - Gửi yêu cầu chấm bài tới AI (Ưu tiên Gemini, dự phòng GitHub Models).
    - Tự động tính toán lại tổng điểm và số câu đúng/sai dựa trên kết quả AI.
    - Cập nhật chi tiết từng câu trả lời với điểm số và nhận xét từ AI.
- **AttemptAiGradingScheduler**:
    - Một trình lập lịch chạy ngầm mỗi **15 giây**.
    - Tìm kiếm các bài thi đang bị kẹt ở trạng thái `GRADING` (do server restart hoặc lỗi luồng async) để thực hiện chấm lại.
    - Đảm bảo 100% bài thi sẽ được chấm ngay cả khi người dùng không online.

---

## 2. Luồng Hoạt Động Hiện Tại

1.  **Nộp bài**: Người dùng nhấn nút nộp bài trên Client.
2.  **Tiếp nhận (Server)**: 
    - `AttemptServiceImpl` nhận yêu cầu, lưu các câu trả lời thô vào DB.
    - Cập nhật trạng thái lượt làm bài thành **`GRADING`**.
    - Gửi phản hồi ngay lập tức cho Client: *"Nộp bài thành công! Bài làm của bạn đang được chấm..."*.
3.  **Chấm bài (Background)**:
    - Một luồng không đồng bộ (`@Async`) được kích hoạt ngay lập tức.
    - **Bước 3.1**: Gom toàn bộ câu hỏi (Trắc nghiệm + Tự luận) và đáp án mẫu gửi tới AI.
    - **Bước 3.2**: AI chấm điểm dựa trên ngữ cảnh toàn bộ đề thi.
    - **Bước 3.3**: Nếu Gemini lỗi, hệ thống tự động chuyển sang gọi GitHub Models (GPT-4o).
    - **Bước 3.4**: Cập nhật kết quả chi tiết từng câu và tổng điểm vào DB.
    - **Bước 3.5**: Chuyển trạng thái bài thi thành **`SUBMITTED`**.
4.  **Dự phòng (Scheduler)**: Nếu bước 3 bị gián đoạn, `AttemptAiGradingScheduler` sẽ phát hiện bài thi vẫn ở trạng thái `GRADING` sau 15 giây và thực hiện lại quy trình chấm bài.
5.  **Xem kết quả**: Người dùng có thể xem lại lịch sử làm bài. Nếu trạng thái là `SUBMITTED`, họ sẽ thấy đầy đủ điểm số và nhận xét chi tiết của AI cho từng câu.

---
*Tài liệu được tạo tự động bởi Antigravity AI.*
