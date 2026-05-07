# Hướng Dẫn Chấm Câu Hỏi Tự Luận (Essay Question)

## Tổng Quan

Hệ thống tự động chấm điểm cho câu hỏi tự luận dựa trên độ dài đáp án:
- **Đáp án < 50 ký tự**: Chấm tự động (so sánh trực tiếp với đáp án mẫu)
- **Đáp án >= 50 ký tự**: Xử lý sau (dành cho AI chấm hoặc chấm thủ công)

---

## 1. Cấu Trúc Dữ Liệu

### Entity: EssayAnswer
```java
@Entity
@Table(name = "essay_answers")
public class EssayAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;
    
    @Lob
    private String sampleAnswer;  // ← Đáp án mẫu lưu ở đây
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

### Entity: Answer (Câu trả lời của học sinh)
```java
@Entity
@Table(name = "answers")
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private Attempt attempt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;
    
    @Column(name = "essay_answer", columnDefinition = "TEXT")
    private String essayAnswer;  // ← Đáp án của học sinh
    
    @Column(name = "is_correct")
    private Boolean isCorrect;   // ← true/false/null
    
    private Double score;         // ← Điểm số
    
    @Column(columnDefinition = "TEXT")
    private String correctAnswerSnapshot;  // ← Đáp án mẫu lúc nộp bài
}
```

---

## 2. Luồng Chấm Điểm

### 2.1 Khi Học Sinh Nộp Bài

```
[Học sinh nộp bài]
         ↓
[AttemptService.submitAttempt()]
         ↓
[For each Answer]
         ↓
[Kiểm tra Question Type]
         ↓
    [MCQ] → Chấm tự động (so sánh selected_option_id)
         ↓
    [ESSAY] → Gọi EssayGradingService.gradeEssay()
         ↓
```

### 2.2 Quy Trình Chấm Tự Luận

```
EssayGradingService.gradeEssay(Answer, EssayAnswer, questionScore)
                            ↓
    ┌───────────────────────┴───────────────────────┐
    ↓                                               ↓
[Đáp án trống?]                              [Có đáp án]
    ↓ Yes                                      ↓ No
Score = 0                           Đếm ký tự (trim)
isCorrect = false                              ↓
isPending = false            ┌─────────────────┴─────────────────┐
                             ↓                                   ↓
                    [< 50 ký tự]                        [>= 50 ký tự]
                             ↓                                   ↓
                  So sánh với                    Mark isPending = true
                 đáp án mẫu                   (Cần xử lí sau: AI/thủ công)
                      ↓
        ┌─────────────┴─────────────┐
        ↓                           ↓
    [Giống]                    [Không giống]
        ↓                           ↓
    Score = full            Score = 0
    isCorrect = true        isCorrect = false
    isPending = false       isPending = false
```

---

## 3. Hàm Chấm Điểm

### Service: EssayGradingService

```java
/**
 * Chấm điểm tự luận cho một câu trả lời.
 *
 * Logic:
 * - Nếu < 50 ký tự: so sánh trực tiếp với đáp án mẫu
 *   + Nếu giống: score = full, isCorrect = true
 *   + Nếu khác: score = 0, isCorrect = false
 * - Nếu >= 50 ký tự: mark isPending = true (xử lí sau)
 */
public GradingResult gradeEssay(Answer answer, EssayAnswer sampleAnswer, double questionScore)

/**
 * So sánh hai đáp án.
 * 
 * Chiến lược:
 * 1. Exact match (so sánh trực tiếp sau normalize)
 * 2. Similarity check (Levenshtein distance >= 80%)
 */
public boolean compareAnswers(String studentAnswer, String sampleAnswer)

/**
 * Normalize text: loại bỏ khoảng trắng, chuyển hoa thường, loại bỏ dấu câu.
 */
private String normalizeText(String text)

/**
 * Tính độ tương đồng (0-1).
 */
public double calculateSimilarity(String s1, String s2)

/**
 * Tính Levenshtein distance.
 */
private int levenshteinDistance(String s1, String s2)
```

### GradingResult Object

```java
public static class GradingResult {
    private Double score;                  // Điểm (null nếu cần xử lí sau)
    private Boolean isCorrect;             // true/false/null
    private boolean isPending;             // Có cần xử lí sau không
    private String reason;                 // Lý do chấm
    private Boolean manualGradeRequired;   // Flag cần chấm thủ công
}
```

---

## 4. Các Trường Hợp Chấm Điểm

### Trường Hợp 1: Đáp án rỗng
```
Input:
  essayAnswer = null hoặc ""
  sampleAnswer = "Paris"
  questionScore = 1.0

Output:
  score = 0.0
  isCorrect = false
  isPending = false
  reason = "Đáp án trống"
```

### Trường Hợp 2: Đáp án < 50 ký tự, giống đáp án mẫu
```
Input:
  essayAnswer = "Paris la capitale de la France"  (31 ký tự)
  sampleAnswer = "Paris là thủ đô của Pháp"
  questionScore = 2.0

Output:
  score = 2.0         ← Full điểm
  isCorrect = true
  isPending = false
  reason = "Đáp án khớp với đáp án mẫu (31 ký tự)"
```

### Trường Hợp 3: Đáp án < 50 ký tự, không giống đáp án mẫu
```
Input:
  essayAnswer = "London là thủ đô"  (16 ký tự)
  sampleAnswer = "Paris là thủ đô của Pháp"
  questionScore = 2.0

Output:
  score = 0.0
  isCorrect = false
  isPending = false
  reason = "Đáp án (16 ký tự) không khớp hoàn toàn với đáp án mẫu, cần xử lý hướng khác"
```

### Trường Hợp 4: Đáp án >= 50 ký tự, có đáp án mẫu
```
Input:
  essayAnswer = "Paris là thủ đô của Pháp. Nó là một thành phố lớn với nền văn hóa phong phú."  (87 ký tự)
  sampleAnswer = "Paris là thủ đô"
  questionScore = 2.0

Output:
  score = null        ← Chưa chấm
  isCorrect = null
  isPending = true    ← Cần xử lí sau
  reason = "Đáp án (87 ký tự) không khớp hoàn toàn với đáp án mẫu, cần xử lý hướng khác"
  manualGradeRequired = true
```

### Trường Hợp 5: Đáp án >= 50 ký tự, không có đáp án mẫu
```
Input:
  essayAnswer = "Học sinh giải thích chi tiết về một chủ đề phức tạp. ..."  (100+ ký tự)
  sampleAnswer = null
  questionScore = 5.0

Output:
  score = null
  isCorrect = null
  isPending = true
  reason = "Đáp án đủ dài (100+ ký tự), không có đáp án mẫu để so sánh, cần chấm"
  manualGradeRequired = true
```

---

## 5. So Sánh Đáp Án (< 50 ký tự)

### Quy Trình So Sánh

```
compareAnswers(studentAnswer, sampleAnswer)
                            ↓
            [Normalize cả hai]
                            ↓
        ┌─────────────┬─────────────┐
        ↓             ↓             ↓
    [Null?]      [Exact match?]   [Similarity >= 80%?]
        ↓ Yes        ↓ Yes           ↓ Yes
      false        return true     return true
                                     ↓ No
                                   return false
```

### Ví Dụ So Sánh

#### Ví Dụ 1: Exact Match
```
Student: "Paris là thủ đô của Pháp"
Sample:  "Paris là thủ đô của Pháp"
Result: true (Khớp hoàn toàn)
```

#### Ví Dụ 2: Khác nhau về khoảng trắng/dấu câu
```
Student: "Paris là thủ đô của Pháp."
Sample:  "paris  là  thủ đô của pháp"
Normalize:
  Student → "paris la thu do cua phap"
  Sample  → "paris la thu do cua phap"
Result: true (Khớp sau normalize)
```

#### Ví Dụ 3: Similarity >= 80%
```
Student: "Paris la capital of France"  (gần giống)
Sample:  "Paris is the capital of France"
Similarity: ~85% (Levenshtein distance)
Result: true (Vượt ngưỡng 80%)
```

#### Ví Dụ 4: Similarity < 80%
```
Student: "London is capital"
Sample:  "Paris is the capital of France"
Similarity: ~40%
Result: false (Không đạt ngưỡng)
```

---

## 6. Tích Hợp Vào AttemptService

### Code trong submitAttempt()

```java
if (question.getType() == QuestionType.ESSAY) {
    System.out.println("  Loại câu hỏi: ESSAY (Tự luận)");
    
    // Lấy đáp án mẫu từ DB
    EssayAnswer sample = essayAnswerRepository
        .findByQuestion_IdAndDeletedAtIsNull(question.getId())
        .orElse(null);
    answer.setCorrectAnswerSnapshot(sample != null ? sample.getSampleAnswer() : null);
    
    // Sử dụng EssayGradingService
    EssayGradingService.GradingResult gradingResult = 
        essayGradingService.gradeEssay(answer, sample, questionScore);
    System.out.println("  [ESSAY GRADING] " + gradingResult);
    
    answer.setSelectedOption(null);
    
    // Nếu chấm tự động (< 50 ký tự)
    if (!gradingResult.isPending()) {
        answer.setScore(gradingResult.getScore() != null ? gradingResult.getScore() : 0d);
        answer.setIsCorrect(gradingResult.getIsCorrect());
        System.out.println("  => Score = " + answer.getScore() + ", Correct = " + answer.getIsCorrect());
        
        if (gradingResult.getIsCorrect()) {
            correctCount++;
            totalScore += questionScore;
        } else {
            wrongCount++;
        }
    } else {
        // Nếu cần xử lí sau (>= 50 ký tự)
        answer.setScore(0d);          // Tạm điểm 0
        answer.setIsCorrect(null);    // Chưa chấm
        System.out.println("  => Cần xử lí sau (AI/thủ công)");
        // TODO: Đánh dấu cần chấm, gửi tới queue AI chấm, etc.
    }
}
```

---

## 7. Hướng Mở Rộng

### 7.1 Thêm Trường "Cần Chấm" Vào Answer
```sql
ALTER TABLE answers ADD COLUMN pending_ai_grading BOOLEAN DEFAULT false;
```

```java
@Column(name = "pending_ai_grading")
private Boolean pendingAiGrading;  // true = cần chấm bằng AI
```

### 7.2 Tạo Queue/Job cho AI Chấm
```java
if (gradingResult.isPending()) {
    answer.setPendingAiGrading(true);
    // Gửi tới AI grading queue
    aiGradingQueue.push(new AiGradingTask(answer.getId(), answer.getEssayAnswer(), sampleAnswer));
}
```

### 7.3 API Để Chấm Thủ Công Sau
```
PUT /api/attempts/{attemptId}/answers/{answerId}/grade
Request:
{
  "score": 1.5,
  "isCorrect": true,
  "notes": "Đáp án tốt"
}
```

---

## 8. Configuration

### application.properties
```properties
# Essay Grading Configuration
app.essay.character-threshold=50
app.essay.similarity-threshold=0.8

# AI Grading (optional)
app.ai-grading.enabled=false
app.ai-grading.api-url=https://api.ai-grading.example.com
app.ai-grading.api-key=xxx
```

---

## 9. Testing

### Unit Test cho compareAnswers()
```java
@Test
void testExactMatch() {
    boolean result = essayGradingService.compareAnswers(
        "Paris là thủ đô",
        "Paris là thủ đô"
    );
    assertTrue(result);
}

@Test
void testNormalizeMatch() {
    boolean result = essayGradingService.compareAnswers(
        "Paris là thủ đô.",
        "paris  là  thủ đô"
    );
    assertTrue(result);
}

@Test
void testSimilarityMatch() {
    boolean result = essayGradingService.compareAnswers(
        "Paris is capital of France",
        "Paris is the capital of France"
    );
    assertTrue(result);  // Similarity > 80%
}

@Test
void testNoMatch() {
    boolean result = essayGradingService.compareAnswers(
        "London is capital",
        "Paris is capital of France"
    );
    assertFalse(result);
}
```

---

## 10. Tóm Tắt Luồng Xử Lý

| Trường Hợp | Ký Tự | Action | Score | isCorrect | isPending |
|-----------|-------|--------|-------|-----------|-----------|
| Rỗng | 0 | Chấm 0 | 0 | false | false |
| Ngắn, giống mẫu | <50 | So sánh → Giống | Full | true | false |
| Ngắn, khác mẫu | <50 | So sánh → Khác | 0 | false | false |
| Dài | >=50 | Chờ xử lí | null | null | true |

---

## 11. Liên Hệ

- **Service**: `EssayGradingService`
- **Integration**: `AttemptServiceImpl.submitAttempt()`
- **Entities**: `Answer`, `EssayAnswer`
- **Repository**: `EssayAnswerRepository`

---

**Cập nhật:** 2026-05-07

