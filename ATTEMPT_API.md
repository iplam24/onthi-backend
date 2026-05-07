# Attempt API (Anti-Cheating)

## Security
All endpoints require JWT login.

- `POST /api/attempts/start`
- `POST /api/attempts/{attemptId}/submit`
- `GET /api/attempts/{attemptId}`
- `GET /api/attempts/me`

## 1) Start Attempt

`POST /api/attempts/start`

```json
{
  "examId": 1
}
```

## 2) Submit Attempt

`POST /api/attempts/{attemptId}/submit`

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

## 3) Get Attempt Detail

`GET /api/attempts/{attemptId}`

## 4) Get My Attempt History

`GET /api/attempts/me`

## Anti-cheat rules implemented
- Reject start if exam not active/outside exam time window.
- Reject start if user already has one `DOING` attempt for the same exam.
- Enforce max attempts from exam settings.
- Reject submit if attempt already submitted/expired.
- Enforce server-side deadline using attempt start time + exam duration (and exam end time if earlier).
- Attempts that reach the deadline are automatically marked `EXPIRED` by the backend scheduler.
- Reject answers that contain question IDs outside the exam.
- Reject duplicate `questionId` in submit payload.
- Reject MCQ answer when `selectedOptionId` does not belong to that question.
- Reject ESSAY payloads that send `selectedOptionId`.
- Score is calculated only on server, client cannot override.
- Flag suspicious attempts when `tabSwitchCount >= 5` or `violationScore >= 50`.

