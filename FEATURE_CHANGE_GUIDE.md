# Feature Change Guide - New Exam / AI Grading Updates

Tài liệu này tóm tắt các thay đổi mới để bạn sửa nhanh sau này.

---

## 1. AI chấm tự luận

### Luồng chấm
1. Nếu đáp án ngắn: chấm local theo `sampleAnswer`
2. Nếu đáp án dài: gọi Gemini
3. Gemini lỗi/quota/429: fallback GitHub Models
4. Nếu cả hai lỗi: lưu tạm `score = 0`, `isCorrect = null`

### File chính
- `src/main/java/com/onthi/v_edu/attempt/service/GeminiAiGradingService.java`
- `src/main/java/com/onthi/v_edu/attempt/service/GitHubModelsAiGradingService.java`
- `src/main/java/com/onthi/v_edu/attempt/service/AttemptServiceImpl.java`

### Field lưu vào Answer
- `aiFeedback`
- `aiGradingMethod`

### Trả ra client
- `feedback`
- `gradingMethod`

---

## 2. Cấu hình AI

### `application.properties`
```properties
app.gemini.api-key=${GEMINI_API_KEY:}
app.gemini.model=${GEMINI_MODEL:gemini-1.5-flash}
app.gemini.enabled=true

app.github-models.api-key=${GITHUB_MODELS_API_KEY:${GITHUB_TOKEN:}}
app.github-models.endpoint=${GITHUB_MODELS_ENDPOINT:https://models.github.ai/inference/chat/completions}
app.github-models.model=${GITHUB_MODELS_MODEL:gpt-4o}
app.github-models.enabled=${GITHUB_MODELS_ENABLED:true}
```

### Lưu ý
- Không hardcode token/key trong source.
- Nếu lộ secret thì revoke/rotate ngay.

---

## 3. Trả feedback cho client

### DTO
- `AttemptAnswerResponse`
  - `feedback`
  - `gradingMethod`

### Ý nghĩa
- `feedback`: nhận xét chi tiết của AI hoặc local grading
- `gradingMethod`: nguồn chấm, ví dụ:
  - `LOCAL`
  - `Chấm bằng Gemini AI`
  - `Chấm bằng GitHub Models`
  - `FALLBACK_FAILED`

---

## 4. Nhận diện UI cho đề thi

### DTO mới / field mới
- `ExamResponse.uiLayoutHint`
- `ExamResponse.sections`
- `ExamQuestionItemResponse.questionType`

### `uiLayoutHint`
Có thể là:
- `STANDARD`
- `LITERATURE`
- `ESSAY`
- `MIXED`

### Dùng sao cho frontend
- `LITERATURE`: render UI kiểu Văn / nghị luận
- `ESSAY`: render dạng tự luận là chính
- `MIXED`: tách `sections[]`
- `STANDARD`: layout mặc định

---

## 5. Sections của đề thi

### Cách nhóm
Backend nhóm theo `questionType` và thứ tự câu hỏi để tạo:
- `Phần I - Trắc nghiệm`
- `Phần II - Tự luận`

### DTO mới
`ExamSectionResponse`
- `sectionIndex`
- `title`
- `sectionType`
- `questionCount`
- `totalScore`
- `startOrderIndex`
- `endOrderIndex`
- `questions[]`

### File chính
- `src/main/java/com/onthi/v_edu/exam/dto/ExamSectionResponse.java`
- `src/main/java/com/onthi/v_edu/exam/service/ExamServiceImpl.java`

---

## 6. Mẫu response cần nhớ

### Exam detail
- giữ `questions[]` cũ để không vỡ frontend
- thêm `sections[]` cho layout đẹp hơn
- thêm `uiLayoutHint` để render nhanh

### Attempt detail
- mỗi câu có thể có `feedback`
- mỗi câu có thể có `gradingMethod`

---

## 7. DB / schema

### Answer table
Nên có thêm:
- `ai_feedback`
- `ai_grading_method`

### Subject / exam UI hint
Hiện tại `uiLayoutHint` là derived, chưa cần lưu DB nếu không muốn.

---

## 8. Document tham chiếu
- `EXAM_ATTEMPT_API.md` - tài liệu API chính
- `FEATURE_CHANGE_GUIDE.md` - tóm tắt ngắn để sửa nhanh
- `ESSAY_GRADING_GUIDE.md` - chi tiết logic chấm tự luận

---

## 9. Nếu cần sửa tiếp

Các điểm thường sửa nhất:
- prompt chấm AI
- logic fallback AI
- cách nhóm `sections[]`
- UI hint theo subject
- field feedback ở answer response

