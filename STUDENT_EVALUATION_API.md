# API Changes & Student Evaluation

Tài liệu này tổng hợp các thay đổi API liên quan đến **lịch sử làm bài** và **đánh giá học sinh**.

## 1) API lịch sử làm bài đã mở rộng filter

### `GET /api/attempts/me`

Thay vì chỉ lấy lịch sử làm bài theo user, API này giờ hỗ trợ filter rộng hơn:

- `subjectId` — lọc theo môn học
- `levelId` — lọc theo khối/lớp
- `examId` — lọc theo đề thi
- `status` — lọc theo trạng thái (`DOING`, `SUBMITTED`, `EXPIRED`)
- `flagged` — lọc bài bị đánh dấu vi phạm
- `from` — thời gian bắt đầu
- `to` — thời gian kết thúc
- `keyword` — tìm theo tiêu đề đề thi
- `page`, `size`, `sort` — phân trang

### Ví dụ
```http
GET /api/attempts/me?subjectId=1&levelId=2&status=SUBMITTED&page=0&size=10
GET /api/attempts/me?keyword=toan&from=2026-05-01T00:00:00&to=2026-05-31T23:59:59
```

### Response đã bổ sung
Trong `AttemptSummaryResponse` và `AttemptDetailResponse` đã thêm:
- `subjectId`
- `subjectName`
- `subjectLevelId`
- `subjectLevelName`

Mục đích: để UI hiển thị rõ môn và khối/lớp của từng attempt mà không cần gọi thêm API khác.

---

## 2) API đánh giá học sinh mới

### `GET /api/statistics/me/evaluation`

API này trả về báo cáo tổng hợp cho học sinh hiện tại, dùng để hiển thị kiểu:
- bạn mạnh ở đâu
- bạn yếu ở đâu
- cần cải thiện phần nào
- tiến bộ ra sao
- có giữ nhịp ôn thi đều hay không

### Filter hỗ trợ
API dùng cùng bộ filter với attempt:
- `subjectId`
- `levelId`
- `examId`
- `status`
- `flagged`
- `from`
- `to`
- `keyword`

### Ví dụ
```http
GET /api/statistics/me/evaluation?subjectId=1&from=2026-05-01T00:00:00
GET /api/statistics/me/evaluation?status=SUBMITTED&levelId=3
```

### Response chính
`StudentEvaluationResponse` gồm:
- thông tin user
- tổng số attempt
- số attempt đã hoàn thành
- số câu trả lời
- điểm trung bình
- điểm cao nhất
- điểm gần nhất
- tỷ lệ đúng
- thời gian làm bài trung bình
- `knowledgeScore`
- `speedScore`
- `progressScore`
- `disciplineScore`
- `overallScore`
- `performanceLabel`
- `summary`
- `strengths`
- `weaknesses`
- `recommendations`
- `subjectEvaluations`
- `topicEvaluations`
- `difficultyEvaluations`

### Ý nghĩa các điểm
- **Knowledge**: năng lực kiến thức tổng quan
- **Speed**: tốc độ làm bài
- **Progress**: tiến bộ theo thời gian
- **Discipline**: độ đều đặn / giữ lửa ôn thi
- **Overall**: điểm tổng hợp cuối cùng

---

## 3) Ghi chú triển khai

- API đánh giá học sinh hiện đang lấy dữ liệu theo **attempt + answer + topic + difficulty**.
- Có thể mở rộng thêm report theo giáo viên/lớp nếu cần sau này.
- Nếu sau này thêm dashboard riêng cho admin/teacher, nên tách endpoint riêng để tránh lẫn scope.

---

## 4) Checklist thay đổi đã thực hiện

- [x] Mở rộng filter lịch sử làm bài
- [x] Bổ sung môn/khối vào response attempt
- [x] Thêm API đánh giá học sinh tổng hợp
- [x] Thêm breakdown theo môn, chủ đề và độ khó
- [x] Thêm file tài liệu tổng hợp thay đổi API

---

## 5) Cách hiểu hệ thống đánh giá

Tài liệu này không chỉ mô tả API, mà còn mô tả cách hệ thống diễn giải kết quả học tập theo kiểu:

> “Bạn đang mạnh ở đâu, yếu ở đâu, vì sao yếu, và nên học gì tiếp theo.”

### 5.1 4 trục đánh giá chính

#### Knowledge score
Đo năng lực kiến thức tổng thể của học sinh.

Nguồn dữ liệu:
- điểm trung bình các attempt
- tỷ lệ đúng trên các câu hỏi
- tỷ lệ đúng theo độ khó

Diễn giải:
- cao: nắm chắc kiến thức, làm được phần lớn câu cơ bản và trung bình
- thấp: còn hổng nền tảng, sai nhiều câu cùng dạng

#### Speed score
Đo tốc độ làm bài so với thời gian đề.

Nguồn dữ liệu:
- `durationTaken`
- `exam.duration`

Diễn giải:
- cao: làm nhanh, ít bị trễ giờ
- thấp: làm chậm, dễ mất điểm ở cuối bài

#### Progress score
Đo mức tiến bộ qua các bài gần đây.

Nguồn dữ liệu:
- so sánh điểm gần nhất với các bài trước

Diễn giải:
- cao: đang cải thiện tốt
- thấp: chững lại hoặc giảm phong độ

#### Discipline score
Đo độ đều đặn và mức “giữ lửa ôn thi”.

Nguồn dữ liệu:
- `UserStudyStreak.currentStreak`
- số ngày có hoạt động trong 30 ngày gần nhất

Diễn giải:
- cao: học đều, có thói quen tốt
- thấp: học ngắt quãng, khó giữ nhịp

### 5.2 Điểm tổng hợp

`overallScore` là điểm cuối cùng của học sinh.

Hiện hệ thống đang ưu tiên:
- `knowledgeScore` 50%
- `speedScore` 15%
- `progressScore` 20%
- `disciplineScore` 15%

Mục tiêu là tránh chấm chỉ bằng điểm số thô, mà nhìn cả năng lực, tiến bộ và độ bền ôn thi.

### 5.2.1 Công thức tính điểm tham chiếu

Đây là công thức tham chiếu để backend và frontend cùng hiểu cùng một cách:

#### a) `knowledgeScore`

```text
knowledgeScore = clamp0_100(
  averageScore * 0.55
  + accuracyRate * 0.25
  + difficultyAccuracy * 0.20
)
```

Trong đó:
- `averageScore`: điểm trung bình của các attempt
- `accuracyRate`: tỷ lệ đúng tổng thể
- `difficultyAccuracy`: điểm đúng theo độ khó, thường lấy trung bình có trọng số:
  - EASY = 0.2
  - MEDIUM = 0.3
  - HARD = 0.5

#### b) `speedScore`

```text
speedScore = clamp0_100(100 - (durationTaken / examDurationInSeconds) * 100)
```

Trong đó:
- `durationTaken`: thời gian học sinh đã làm bài
- `examDurationInSeconds`: thời gian đề quy đổi ra giây

Ý nghĩa:
- làm càng nhanh thì điểm càng cao
- làm quá thời gian thì điểm giảm mạnh

#### c) `progressScore`

```text
progressScore = clamp0_100(50 + (avgRecent - avgPrevious))
```

Trong đó:
- `avgRecent`: trung bình 3–5 bài gần nhất
- `avgPrevious`: trung bình 3–5 bài trước đó

Ý nghĩa:
- tăng điểm nếu học sinh đang đi lên
- giảm điểm nếu kết quả chững lại hoặc đi xuống

#### d) `disciplineScore`

```text
streakScore = clamp0_100(currentStreak * 10)
consistencyScore = clamp0_100(activeDaysLast30 * 7)
disciplineScore = clamp0_100(streakScore * 0.65 + consistencyScore * 0.35)
```

Trong đó:
- `currentStreak`: số ngày học liên tiếp
- `activeDaysLast30`: số ngày có hoạt động trong 30 ngày gần nhất

Ý nghĩa:
- học đều thì điểm cao
- học ngắt quãng thì điểm thấp

#### e) `overallScore`

```text
overallScore = clamp0_100(
  knowledgeScore * 0.50
  + speedScore * 0.15
  + progressScore * 0.20
  + disciplineScore * 0.15
)
```

Đây là điểm tổng hợp cuối cùng để xếp loại học sinh.

#### f) Hàm clamp

```text
clamp0_100(x) = min(100, max(0, x))
```

Mục đích:
- tránh điểm âm
- tránh vượt quá 100

### 5.2.2 Quy tắc sinh `performanceLabel`

```text
90+  -> Xuất sắc
75-89 -> Tốt
60-74 -> Khá
40-59 -> Cần cải thiện
< 40  -> Cảnh báo
```

### 5.2.3 Quy tắc sinh `strengths`, `weaknesses`, `recommendations`

#### `strengths`
Sinh từ các điều kiện:
- `knowledgeScore >= 75` -> có nền tảng tốt
- môn có `averageScore` cao nhất -> môn mạnh nhất
- `speedScore >= 70` -> làm bài nhanh
- `progressScore >= 70` -> đang tiến bộ
- `disciplineScore >= 70` -> học đều, giữ lửa tốt

#### `weaknesses`
Sinh từ các điều kiện:
- `knowledgeScore < 60` -> hổng kiến thức
- môn có `averageScore < 60` -> yếu theo môn
- chủ đề có `accuracyRate < 60` -> yếu theo chủ đề
- `HARD accuracyRate < 50` -> yếu câu khó
- `speedScore < 60` -> làm bài chậm
- `disciplineScore < 60` -> học chưa đều
- `progressScore < 50` -> chưa tiến bộ rõ

#### `recommendations`
Sinh theo rule ưu tiên:
1. Ôn môn yếu nhất trước
2. Luyện riêng chủ đề yếu nhất
3. Chỉ tăng độ khó khi EASY/MEDIUM đã ổn
4. Làm bài bấm giờ nếu `speedScore` thấp
5. So sánh 5 bài gần nhất nếu `progressScore` thấp
6. Giữ streak hằng ngày nếu `disciplineScore` thấp

### 5.3 Nhãn xếp loại

`performanceLabel` được quy đổi theo `overallScore`:

- `90+` → **Xuất sắc**
- `75–89` → **Tốt**
- `60–74` → **Khá**
- `40–59` → **Cần cải thiện**
- `< 40` → **Cảnh báo**

### 5.4 Cách đọc `strengths`, `weaknesses`, `recommendations`

#### `strengths`
Danh sách điểm mạnh nổi bật.

Ví dụ:
- “Mạnh nhất ở môn Toán.”
- “Tốc độ làm bài đang ổn.”

#### `weaknesses`
Danh sách điểm yếu cần chú ý.

Ví dụ:
- “Bạn đang yếu hơn ở môn Hóa.”
- “Chủ đề Hàm số là điểm yếu rõ nhất hiện tại.”
- “Câu khó / vận dụng cao còn yếu.”

#### `recommendations`
Danh sách gợi ý hành động tiếp theo.

Ví dụ:
- “Ôn tập lại môn Toán bằng đề ngắn và câu cơ bản trước.”
- “Luyện riêng chủ đề Hình học để kéo tỷ lệ đúng lên.”
- “Giữ chuỗi ôn thi hằng ngày để có dữ liệu tiến bộ ổn định hơn.”

---

## 6) Ý nghĩa các response con

### `SubjectEvaluationResponse`
Đánh giá theo môn học.

Fields chính:
- `subjectId`, `subjectName`
- `levelId`, `levelName`
- `attemptCount`
- `averageScore`
- `accuracyRate`
- `bestScore`
- `latestScore`
- `averageDurationSeconds`

Khi dùng card UI, đây là phần phù hợp để hiển thị:
- môn mạnh nhất
- môn yếu nhất
- môn cần ôn trước

### `TopicEvaluationResponse`
Đánh giá theo chủ đề.

Fields chính:
- `topicId`, `topicName`
- `subjectId`, `subjectName`
- `totalAnswers`
- `correctAnswers`
- `accuracyRate`
- `averageScore`

Khi dùng card UI, đây là phần phù hợp để hiển thị:
- chủ đề sai nhiều nhất
- chủ đề cần luyện riêng

### `DifficultyEvaluationResponse`
Đánh giá theo độ khó.

Fields chính:
- `difficulty` (`EASY`, `MEDIUM`, `HARD`)
- `totalAnswers`
- `correctAnswers`
- `accuracyRate`
- `averageScore`

Khi dùng card UI, đây là phần phù hợp để hiển thị:
- làm tốt câu dễ chưa
- trung bình có ổn không
- câu khó còn yếu đến mức nào

---

## 7) Ví dụ phản hồi ngắn gọn để UI hiển thị

### Trường hợp học sinh làm tốt

> Bạn đang mạnh ở môn Toán, tốc độ làm bài ổn và điểm số có xu hướng tăng. Tuy nhiên, hãy tiếp tục giữ nhịp học đều để phong độ không bị gián đoạn.

### Trường hợp học sinh cần cải thiện

> Bạn đang yếu ở môn Hóa và chủ đề Hàm số. Điểm số gần đây chưa tăng rõ rệt, đồng thời tốc độ làm bài còn chậm. Nên ôn lại câu cơ bản trước rồi mới tăng độ khó.

### Trường hợp học sinh thiếu kỷ luật

> Bạn có kiến thức ở mức tạm ổn nhưng học chưa đều, nên kết quả chưa ổn định. Hãy giữ chuỗi ôn thi mỗi ngày để hệ thống đánh giá chính xác hơn và giúp bạn tiến bộ bền vững.

---

## 8) Gợi ý hiển thị trên frontend

### Màn hình student profile
Nên chia thành 4 khối:
1. Điểm tổng hợp
2. Biểu đồ 4 trục đánh giá
3. Môn/chủ đề yếu nhất
4. Gợi ý ôn tập tiếp theo

### Màn hình teacher/admin
Nên hiển thị thêm:
- lọc theo môn
- lọc theo khối/lớp
- lọc theo khoảng thời gian
- lọc theo đề thi
- danh sách học sinh có nguy cơ yếu

### Màu gợi ý
- Xanh: `75+`
- Vàng: `60–74`
- Cam: `40–59`
- Đỏ: `<40`

---

## 9) Ví dụ JSON rút gọn

```json
{
  "status": 200,
  "message": "Lấy đánh giá học sinh thành công!",
  "data": {
	"userId": 12,
	"username": "student01",
	"fullName": "Nguyễn Văn A",
	"levelName": "Lớp 12",
	"totalAttempts": 18,
	"averageScore": 68.5,
	"bestScore": 92,
	"latestScore": 74,
	"knowledgeScore": 70.2,
	"speedScore": 58.1,
	"progressScore": 76.4,
	"disciplineScore": 64.0,
	"overallScore": 68.7,
	"performanceLabel": "Khá",
	"summary": "Hồ sơ học tập của student01: mạnh hơn ở môn Toán, cần cải thiện môn Hóa, yếu rõ ở chủ đề Hàm số, điểm tổng hợp hiện tại là 68.7/100.",
	"weaknesses": [
	  "Bạn đang yếu hơn ở môn Hóa.",
	  "Chủ đề Hàm số là điểm yếu rõ nhất hiện tại."
	],
	"recommendations": [
	  "Ôn tập lại môn Hóa bằng đề ngắn và câu cơ bản trước.",
	  "Luyện riêng chủ đề Hàm số để kéo tỷ lệ đúng lên."
	]
  }
}
```

---

## 10) Tóm tắt cho team

Nếu chỉ nhớ 1 câu, thì hệ thống này đánh giá theo logic:

> **Kiến thức + tốc độ + tiến bộ + kỷ luật = đánh giá học sinh toàn diện**

Và đầu ra cần trả lời được:
- học sinh mạnh ở đâu
- học sinh yếu ở đâu
- đang tiến bộ hay không
- cần học gì tiếp theo

