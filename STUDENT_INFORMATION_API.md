# Student Information API

Tài liệu này mô tả riêng phần **thông tin học sinh** và **giữ lửa ôn thi** trong hệ thống.

Mục tiêu của nhóm API này là:
- xem hồ sơ cá nhân của học sinh
- cập nhật thông tin hồ sơ
- xem trạng thái giữ lửa ôn thi
- check-in để tăng streak mỗi ngày

---

## 1) Tổng quan endpoint

Tất cả API trong nhóm này đều yêu cầu đăng nhập.

### 1. `GET /api/users/me`
Lấy hồ sơ hiện tại của học sinh.

### 2. `PUT /api/users/me/information`
Cập nhật thông tin cá nhân của học sinh.

### 3. `GET /api/users/me/streak`
Lấy thông tin giữ lửa ôn thi hiện tại.

### 4. `POST /api/users/me/streak/check-in`
Check-in streak trong ngày, dùng khi học sinh học bài / làm đề để tăng nhịp ôn thi.

---

## 2) Wrapper response

Tất cả endpoint đều trả về theo dạng:

```json
{
  "status": 200,
  "message": "...",
  "data": { }
}
```

Trong đó:
- `status`: HTTP status code
- `message`: thông báo nghiệp vụ
- `data`: dữ liệu thực tế

---

## 3) DTO / field chính

### 3.1 `UserInformationRequest`
Dùng cho API cập nhật thông tin.

```java
record UserInformationRequest(
    String fullName,
    String schoolName,
    Integer levelId,
    LocalDate dob,
    String avatar
) {}
```

#### Ý nghĩa field
- `fullName`: họ và tên học sinh
- `schoolName`: tên trường
- `levelId`: khối/lớp hiện tại
- `dob`: ngày sinh
- `avatar`: ảnh đại diện (URL)

#### Ghi chú
- Tối thiểu gửi **1 field có giá trị** khi update.
- Các field có thể cập nhật riêng lẻ, không bắt buộc gửi đầy đủ.
- `levelId` được backend kiểm tra tồn tại trong bảng `levels`.

---

### 3.2 `UserProfileResponse`
Dùng cho API lấy hồ sơ và cập nhật hồ sơ.

```java
record UserProfileResponse(
    Integer id,
    String username,
    String email,
    String roleName,
    String fullName,
    String schoolName,
    Integer levelId,
    String levelName,
    LocalDate dob,
    String avatar,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    UserStreakResponse streak
) {}
```

#### Ý nghĩa field
- `id`: id người dùng
- `username`: tên đăng nhập
- `email`: email
- `roleName`: vai trò hệ thống
- `fullName`: họ và tên hiển thị
- `schoolName`: tên trường
- `levelId`: id khối/lớp
- `levelName`: tên khối/lớp
- `dob`: ngày sinh
- `avatar`: ảnh đại diện
- `createdAt`: thời điểm tạo tài khoản
- `updatedAt`: thời điểm cập nhật thông tin gần nhất
- `streak`: thông tin giữ lửa ôn thi của học sinh

#### Ghi chú frontend
- `streak` được trả luôn trong profile, nên không cần gọi thêm API streak nếu chỉ muốn hiển thị nhanh.
- Các field như `fullName`, `schoolName`, `avatar` có thể `null` nếu học sinh chưa cập nhật.

---

### 3.3 `UserStreakResponse`
Dùng cho API streak và cũng nằm trong profile.

```java
record UserStreakResponse(
    Integer currentStreak,
    Integer longestStreak,
    LocalDate lastActiveDate,
    boolean activeToday,
    Integer fireLevel
) {}
```

#### Ý nghĩa field
- `currentStreak`: số ngày học liên tiếp hiện tại
- `longestStreak`: chuỗi ngày học dài nhất từ trước đến nay
- `lastActiveDate`: ngày học gần nhất
- `activeToday`: có học trong hôm nay hay không
- `fireLevel`: mức “giữ lửa ôn thi” do backend quy đổi sẵn

#### Quy ước `fireLevel`
Backend đang quy đổi theo thang 0–5:
- `0`: chưa có streak
- `1`: mới bắt đầu
- `2`: ổn định nhẹ
- `3`: có nhịp học khá
- `4`: giữ lửa tốt
- `5`: bùng cháy / rất đều

#### Ghi chú frontend
- `activeToday` là field backend tính sẵn, frontend không cần tự suy ra.
- `fireLevel` nên dùng để hiển thị badge, icon lửa, hoặc progress ring.

---

## 4) Mô tả từng API

### 4.1 `GET /api/users/me`
Lấy hồ sơ hiện tại của học sinh.

#### Response data
Trả về `UserProfileResponse`.

#### Ví dụ
```http
GET /api/users/me
```

```json
{
  "status": 200,
  "message": "Lấy hồ sơ người dùng thành công!",
  "data": {
    "id": 12,
    "username": "student01",
    "email": "student01@gmail.com",
    "roleName": "STUDENT",
    "fullName": "Nguyễn Văn A",
    "schoolName": "THPT ABC",
    "levelId": 3,
    "levelName": "Lớp 12",
    "dob": "2008-03-12",
    "avatar": "https://.../avatar.jpg",
    "createdAt": "2026-04-22T10:11:12",
    "updatedAt": "2026-05-06T09:30:00",
    "streak": {
      "currentStreak": 6,
      "longestStreak": 10,
      "lastActiveDate": "2026-05-06",
      "activeToday": true,
      "fireLevel": 2
    }
  }
}
```

---

### 4.2 `PUT /api/users/me/information`
Cập nhật thông tin cá nhân.

#### Request body
```json
{
  "fullName": "Nguyễn Văn A",
  "schoolName": "THPT ABC",
  "levelId": 3,
  "dob": "2008-03-12",
  "avatar": "https://.../avatar.jpg"
}
```

#### Rules
- Chỉ cần gửi các field muốn thay đổi.
- Nếu `levelId` không hợp lệ, backend trả `404`.
- Nếu request rỗng hoàn toàn, backend trả `400`.

#### Response data
Trả về `UserProfileResponse` sau khi cập nhật.

---

### 4.3 `GET /api/users/me/streak`
Lấy thông tin streak hiện tại.

#### Response data
Trả về `UserStreakResponse`.

#### Ví dụ
```json
{
  "status": 200,
  "message": "Lấy thông tin giữ lửa ôn thi thành công!",
  "data": {
    "currentStreak": 6,
    "longestStreak": 10,
    "lastActiveDate": "2026-05-06",
    "activeToday": true,
    "fireLevel": 2
  }
}
```

---

### 4.4 `POST /api/users/me/streak/check-in`
Check-in streak trong ngày.

#### Mục đích
Endpoint này dùng để ghi nhận hoạt động học tập trong ngày, từ đó:
- tăng `currentStreak`
- cập nhật `lastActiveDate`
- tăng động lực giữ lửa ôn thi

#### Response data
Trả về `UserStreakResponse` mới nhất sau khi check-in.

#### Ví dụ
```json
{
  "status": 200,
  "message": "Giữ lửa ôn thi thành công!",
  "data": {
    "currentStreak": 7,
    "longestStreak": 10,
    "lastActiveDate": "2026-05-06",
    "activeToday": true,
    "fireLevel": 3
  }
}
```

---

## 5) Ý nghĩa nghiệp vụ cho frontend

### 5.1 Hồ sơ học sinh nên hiển thị gì
- Họ tên
- Trường
- Khối/lớp
- Avatar
- Streak hiện tại
- Longest streak
- Badge giữ lửa

### 5.2 Gợi ý UI cho streak
Có thể hiển thị theo các trạng thái:
- `0`: chưa có lửa
- `1–2`: lửa mới nhóm
- `3–6`: đang ấm
- `7–13`: nóng
- `14+`: bùng cháy

### 5.3 Gợi ý hiển thị thông điệp
- “Hôm nay bạn đã giữ lửa ôn thi!”
- “Còn 1 ngày nữa để tăng mức lửa.”
- “Chuỗi học của bạn đang rất ổn, tiếp tục duy trì nhé.”

---

## 6) Quan hệ với phần đánh giá học sinh

Phần `information` này liên quan trực tiếp đến đánh giá học sinh ở chỗ:
- `levelId` dùng để lọc và phân tích theo khối/lớp
- `streak` dùng để tính `disciplineScore`
- `schoolName` giúp hiển thị profile đầy đủ hơn

Nói ngắn gọn:
- `information` = dữ liệu hồ sơ nền tảng
- `streak` = dữ liệu thói quen học tập
- `evaluation` = dữ liệu đánh giá tổng hợp

---

## 7) Checklist

- [x] Có tài liệu riêng cho phần information học sinh
- [x] Có mô tả đầy đủ endpoint
- [x] Có giải thích field request/response
- [x] Có ví dụ JSON
- [x] Có hướng dẫn hiển thị cho frontend

