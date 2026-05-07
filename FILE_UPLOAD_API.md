# API Upload File/Hình Ảnh

## Endpoint

```
POST /api/files/upload
```

**Base URL:** `https://api.vuxuanlam.me`

**Full URL:** `https://api.vuxuanlam.me/api/files/upload`

---

## Xác Thực

- **Yêu cầu:** Authenticated (cần JWT Token)
- **Header:** 
  ```
  Authorization: Bearer <JWT_TOKEN>
  ```

---

## Đầu Vào (Input)

### Content-Type
```
multipart/form-data
```

### Form Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|--------|------|---------|-------|
| `file` | File (Binary) | Có | File cần upload (ảnh, tài liệu, etc.) |

### Ví dụ Request (cURL)

```bash
curl -X POST "https://api.vuxuanlam.me/api/files/upload" \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "file=@/path/to/image.jpg"
```

### Ví dụ Request (JavaScript/Fetch)

```javascript
const formData = new FormData();
formData.append('file', fileInputElement.files[0]);

fetch('https://api.vuxuanlam.me/api/files/upload', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    // Do NOT set Content-Type header - browser will set it automatically with boundary
  },
  body: formData,
  credentials: 'include' // if needed for cookies
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

### Ví dụ Request (FormData từ HTML Form)

```html
<form id="uploadForm" enctype="multipart/form-data">
  <input type="file" name="file" id="fileInput" required />
  <button type="submit">Upload</button>
</form>

<script>
document.getElementById('uploadForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  
  const formData = new FormData(this);
  const token = localStorage.getItem('token'); // hoặc từ sessionStorage
  
  try {
    const response = await fetch('https://api.vuxuanlam.me/api/files/upload', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token
      },
      body: formData,
      credentials: 'include'
    });
    
    const data = await response.json();
    console.log('Upload response:', data);
    
    if (response.ok) {
      console.log('File URL:', data.data.url);
    } else {
      console.error('Upload failed:', data.message);
    }
  } catch (error) {
    console.error('Error:', error);
  }
});
</script>
```

---

## Đầu Ra (Output)

### HTTP Status Codes

| Status | Ý Nghĩa |
|--------|---------|
| `201` | Created - Upload thành công |
| `400` | Bad Request - File rỗng, tên file không hợp lệ |
| `401` | Unauthorized - Không có/token không hợp lệ |
| `403` | Forbidden - Không có quyền |
| `500` | Internal Server Error - Lỗi server |

### Response Body (Success - 201)

```json
{
  "status": 201,
  "message": "Tải file lên thành công!",
  "data": {
    "originalName": "my-image.jpg",
    "fileName": "2026/05/07/a1b2c3d4e5f6_my-image.jpg",
    "contentType": "image/jpeg",
    "size": 152048,
    "url": "/uploads/2026/05/07/a1b2c3d4e5f6_my-image.jpg",
    "storagePath": "/home/user/app/uploads/2026/05/07/a1b2c3d4e5f6_my-image.jpg",
    "uploadedAt": "2026-05-07T14:30:45"
  }
}
```

### Response Body (Error - 400)

```json
{
  "status": 400,
  "message": "File upload không được rỗng!",
  "data": null
}
```

```json
{
  "status": 400,
  "message": "Tên file không hợp lệ!",
  "data": null
}
```

### Response Body (Error - 401)

```json
{
  "status": 401,
  "message": "Unauthorized",
  "data": null
}
```

### Response Body (Error - 500)

```json
{
  "status": 500,
  "message": "Không thể tải file lên: <error details>",
  "data": null
}
```

---

## Chi Tiết Response Data

### UploadedFileResponse Object

| Trường | Kiểu | Mô tả |
|-------|------|-------|
| `originalName` | String | Tên file gốc khi upload (ví dụ: "my-image.jpg") |
| `fileName` | String | Tên file được lưu trên server (ví dụ: "2026/05/07/a1b2c3d4e5f6_my-image.jpg") |
| `contentType` | String | MIME type của file (ví dụ: "image/jpeg", "image/png", "application/pdf") |
| `size` | Long | Kích thước file tính bằng bytes |
| `url` | String | Đường dẫn public để truy cập file (ví dụ: "/uploads/2026/05/07/a1b2c3d4e5f6_my-image.jpg") |
| `storagePath` | String | Đường dẫn tuyệt đối trên server |
| `uploadedAt` | LocalDateTime | Thời gian upload (ISO 8601 format: "2026-05-07T14:30:45") |

---

## Lưu Ý Quan Trọng

### File Path Structure
- Files được lưu theo cấu trúc: `uploads/YYYY/MM/DD/UUID_originalname.ext`
- Ví dụ: `uploads/2026/05/07/a1b2c3d4e5f6_my-image.jpg`
- UUID được random để tránh collision

### File URL
- URL public được build từ: `/uploads/<fileName>`
- Ví dụ: `/uploads/2026/05/07/a1b2c3d4e5f6_my-image.jpg`
- Nếu có front-end server, URL này có thể được map hoặc proxy

### Content-Type Header
- **Quan trọng:** Khi dùng FormData (JavaScript), KHÔNG set `Content-Type` header thủ công
- Browser sẽ tự động set `Content-Type: multipart/form-data; boundary=...`
- Nếu bạn set thủ công mà không đúng, server sẽ reject

### CORS
- Endpoint yêu cầu CORS được cấu hình cho các origin:
  - `https://onthi.vuxuanlam.me`
  - `https://admin.vuxuanlam.me`
  - `https://api.vuxuanlam.me`
  - `http://localhost:3000` (dev)

### Token
- Token phải được gửi trong header `Authorization: Bearer <token>`
- Token được trả lại từ endpoint login: `POST /api/auth/login`

---

## Ví dụ Sử Dụng Thực Tế

### Ví dụ 1: Upload từ Admin Dashboard

```typescript
// admin.vuxuanlam.me

async function uploadImage(file: File, token: string) {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await fetch('https://api.vuxuanlam.me/api/files/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData,
      credentials: 'include' // for cookies if needed
    });

    const result = await response.json();

    if (response.ok) {
      console.log('Upload successful!');
      console.log('Public URL:', result.data.url);
      console.log('File size:', result.data.size, 'bytes');
      console.log('Uploaded at:', result.data.uploadedAt);
      
      // Use the URL to display the image
      const img = document.createElement('img');
      img.src = 'https://api.vuxuanlam.me' + result.data.url;
      document.body.appendChild(img);
    } else {
      console.error('Upload failed:', result.message);
    }
  } catch (error) {
    console.error('Network error:', error);
  }
}

// Usage
const fileInput = document.getElementById('fileInput') as HTMLInputElement;
const token = localStorage.getItem('jwt_token');
uploadImage(fileInput.files[0], token);
```

### Ví dụ 2: Upload từ Ôn Thi Dashboard

```typescript
// onthi.vuxuanlam.me

async function uploadExamImage(file: File) {
  const token = sessionStorage.getItem('auth_token');
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('https://api.vuxuanlam.me/api/files/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  const json = await response.json();
  
  if (response.ok) {
    return json.data.url; // Trả về URL để lưu vào database
  } else {
    throw new Error(json.message);
  }
}

// Usage in exam question editor
document.getElementById('insertImageBtn').addEventListener('click', async () => {
  const file = prompt('Select image file...');
  if (file) {
    try {
      const imageUrl = await uploadExamImage(file);
      // Insert into rich text editor
      editor.insertImage(imageUrl);
    } catch (error) {
      alert('Upload failed: ' + error.message);
    }
  }
});
```

### Ví dụ 3: Upload Multiple Files

```typescript
async function uploadMultipleFiles(files: FileList, token: string) {
  const uploadedUrls = [];

  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('https://api.vuxuanlam.me/api/files/upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      const result = await response.json();
      
      if (response.ok) {
        uploadedUrls.push({
          fileName: result.data.originalName,
          url: result.data.url
        });
      } else {
        console.error(`Failed to upload ${file.name}:`, result.message);
      }
    } catch (error) {
      console.error(`Error uploading ${file.name}:`, error);
    }
  }

  return uploadedUrls;
}
```

---

## Troubleshooting

### CORS Error: "No 'Access-Control-Allow-Origin' header"
- Kiểm tra origin của frontend (phải từ một trong các domain được allow)
- Đảm bảo request có `Authorization` header với token hợp lệ
- Preflight (OPTIONS) phải được server cho phép

### 401 Unauthorized
- Token đã hết hạn hoặc không hợp lệ
- Hãy login lại để lấy token mới

### 400 Bad Request
- File rỗng (kiểm tra fileInput.files[0])
- Tên file chứa ký tự không hợp lệ

### File không được lưu
- Kiểm tra quyền write trên thư mục `uploads`
- Kiểm tra dung lượng ổ đĩa
- Kiểm tra logs server

---

## Tệp Liên Quan

- **Controller:** `com.onthi.v_edu.common.fileupload.controller.FileUpLoadController`
- **Service:** `com.onthi.v_edu.common.fileupload.service.FileUploadServiceImpl`
- **DTO:** `com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse`
- **Base Response:** `com.onthi.v_edu.common.dto.ApiResponse`

---

**Cập nhật cuối:** 2026-05-07

