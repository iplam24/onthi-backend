# Question Edit Guide

Tài liệu này mô tả cách **sửa câu hỏi** trong hệ thống hiện tại, dành cho admin hoặc frontend form nhập liệu.

---

## 1) Endpoint sửa câu hỏi

### `PUT /api/questions/{id}`

Đây là API sửa câu hỏi theo kiểu **full update**:
- lấy câu hỏi hiện có
- kiểm tra `topicId`
- kiểm tra `type`
- cập nhật lại nội dung chính
- xoá dữ liệu con cũ của câu hỏi
- tạo lại dữ liệu con mới theo `type`

### Quyền
- yêu cầu đăng nhập
- chỉ `ROLE_ADMIN` mới được gọi

---

## 2) Request body

Dùng chung DTO với tạo câu hỏi:

```json
{
  "content": "...",
  "contentFormat": "LATEX",
  "url": "...",
  "type": "MCQ",
  "difficulty": "MEDIUM",
  "topicId": 1,
  "options": [
    {
      "content": "A",
      "isCorrect": false
    },
    {
      "content": "B",
      "isCorrect": true
    }
  ],
  "sampleAnswer": "...",
  "explanation": "..."
}
```

---

## 2.1) Hiển thị công thức toán đẹp

Nếu câu hỏi có:
- căn bậc hai
- phân số
- số mũ
- tổng / tích phân / ký hiệu toán

thì nên gửi `contentFormat = LATEX` và viết nội dung theo chuẩn LaTeX.

### Ví dụ

```text
Tính \( x^2 + \frac{1}{2} \) khi \( x = 3 \)
```

Frontend sẽ render bằng KaTeX hoặc MathJax để hiển thị đẹp.

### Quy ước khuyến nghị
- `contentFormat = PLAIN_TEXT` cho nội dung thường
- `contentFormat = LATEX` cho nội dung có công thức toán

Lưu ý:
- backend chỉ lưu raw text/LaTeX
- frontend chịu trách nhiệm render đẹp

---

## 3) Rule cập nhật theo loại câu hỏi

### 3.1 Nếu `type = MCQ`

Bắt buộc:
- `content`
- `difficulty`
- `topicId`
- `options` với ít nhất 2 đáp án
- ít nhất 1 đáp án đúng

Ghi chú:
- toàn bộ option cũ sẽ bị xoá rồi tạo lại
- `sampleAnswer` không dùng cho MCQ
- `explanation` vẫn được lưu nếu có

### 3.2 Nếu `type = ESSAY`

Bắt buộc:
- `content`
- `difficulty`
- `topicId`
- `sampleAnswer`

Ghi chú:
- toàn bộ option cũ sẽ bị xoá
- `sampleAnswer` là đáp án mẫu của câu tự luận
- `explanation` vẫn được lưu nếu có

---

## 4) Hành vi khi sửa câu hỏi

### Trường hợp 1: sửa nội dung nhưng không đổi loại
- đổi `content`
- đổi `difficulty`
- đổi `topicId`
- thay option mới hoặc sample answer mới tuỳ loại

### Trường hợp 2: đổi từ MCQ sang ESSAY
- option cũ bị xoá
- hệ thống tạo `EssayAnswer`
- cần gửi `sampleAnswer`

### Trường hợp 3: đổi từ ESSAY sang MCQ
- essay cũ bị xoá
- hệ thống tạo lại danh sách option
- cần gửi `options` hợp lệ

### Trường hợp 4: sửa `explanation`
- nếu gửi chuỗi rỗng / chỉ có khoảng trắng → explanation cũ bị xoá
- nếu gửi nội dung mới → explanation được tạo hoặc cập nhật lại

---

## 5) Những lỗi hay gặp

### MCQ không đủ đáp án
```json
{
  "message": "Câu hỏi MCQ phải có ít nhất 2 đáp án!"
}
```

### MCQ không có đáp án đúng
```json
{
  "message": "Câu hỏi MCQ phải có ít nhất 1 đáp án đúng!"
}
```

### ESSAY thiếu đáp án mẫu
```json
{
  "message": "Câu hỏi ESSAY phải có đáp án mẫu!"
}
```

### Topic không tồn tại
```json
{
  "message": "Không tìm thấy topic!"
}
```

---

## 6) Gợi ý cho frontend

### Form edit MCQ nên có
- ô nội dung câu hỏi
- chọn `contentFormat`
- ô URL ảnh / media nếu có
- dropdown độ khó
- dropdown chủ đề
- danh sách option động
- checkbox đánh dấu đáp án đúng
- ô giải thích

### Form edit ESSAY nên có
- ô nội dung câu hỏi
- chọn `contentFormat`
- ô URL ảnh / media nếu có
- dropdown độ khó
- dropdown chủ đề
- ô đáp án mẫu
- ô giải thích

### Mẹo UI
- khi chuyển type MCQ <-> ESSAY, nên cảnh báo người dùng rằng dữ liệu con cũ sẽ bị thay thế
- nên có preview trước khi lưu để tránh mất option/đáp án mẫu

---

## 7) Cách làm nội dung toán đẹp

Nếu câu hỏi có số mũ, phân số, căn, biểu thức toán học:
- nên lưu nội dung ở dạng LaTeX raw text
- frontend render bằng KaTeX hoặc MathJax

Ví dụ:
```text
Tính \( x^2 + \frac{1}{2} \)
```

---

## 8) Checklist khi sửa câu hỏi

- [x] Kiểm tra `topicId` tồn tại
- [x] Kiểm tra `type` hợp lệ
- [x] Nếu MCQ: có ít nhất 2 option
- [x] Nếu MCQ: có ít nhất 1 đáp án đúng
- [x] Nếu ESSAY: có `sampleAnswer`
- [x] Có `explanation` thì lưu, rỗng thì xoá
- [x] Nếu đổi type thì dữ liệu con cũ sẽ bị xoá và tạo lại

