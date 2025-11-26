# API Documentation - Panchayat Album & Gallery Management

**Base URL:** `/api/v1/panchayat`  
**Authentication:** Requires `PANCHAYAT_ADMIN` role  
**Content Type:** `application/x-www-form-urlencoded` or `multipart/form-data` for file uploads

---

## Table of Contents
1. [Album APIs](#album-apis)
2. [Gallery Image APIs](#gallery-image-apis)
3. [Response Format](#response-format)
4. [Common Parameters](#common-parameters)

---

## Album APIs

### Base Endpoint: `/albums`

---

### 1. Create Album
**Endpoint:** `POST /api/v1/panchayat/albums`  
**Content-Type:** `multipart/form-data`  
**Purpose:** Create a new album with optional cover image upload

#### Request Parameters (Form Data)
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `albumName` | String | ✅ Yes | - | Name of the album |
| `description` | String | ❌ No | null | Album description |
| `coverImageFile` | File | ❌ No | null | Cover image file to upload and compress |
| `compressionQuality` | String | ❌ No | HIGH | Compression quality: `HIGH`, `MEDIUM`, or `LOW` |
| `coverImageUrl` | String | ❌ No | null | Direct URL to cover image (if not uploading file) |

#### Request Example (cURL - with file upload)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer <token>" \
  -F "albumName=My Album" \
  -F "description=Album description" \
  -F "coverImageFile=@/path/to/image.jpg" \
  -F "compressionQuality=HIGH"
```

#### Request Example (Form Data - URL only)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer <token>" \
  -F "albumName=My Album" \
  -F "description=Album description" \
  -F "coverImageUrl=https://example.com/image.jpg"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Album created successfully",
  "data": {
    "id": 1,
    "albumName": "My Album",
    "description": "Album description",
    "coverImageUrl": "https://b2-storage-url.com/cover-image-presigned-url",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T10:30:00Z"
  }
}
```

---

### 2. Get All Albums
**Endpoint:** `GET /api/v1/panchayat/albums`  
**Purpose:** Retrieve paginated list of all albums

#### Request Parameters (Query)
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | ❌ No | 0 | Page number (0-indexed) |
| `size` | Integer | ❌ No | 20 | Number of albums per page |

#### Request Example
```bash
curl -X GET "http://localhost:8080/api/v1/panchayat/albums?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "albumName": "Album 1",
        "description": "Description 1",
        "coverImageUrl": "https://b2-storage-url.com/...",
        "createdAt": "2025-11-26T10:30:00Z",
        "updatedAt": "2025-11-26T10:30:00Z"
      },
      {
        "id": 2,
        "albumName": "Album 2",
        "description": "Description 2",
        "coverImageUrl": "https://b2-storage-url.com/...",
        "createdAt": "2025-11-26T09:15:00Z",
        "updatedAt": "2025-11-26T09:15:00Z"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 25,
    "totalPages": 3,
    "isFirst": true,
    "isLast": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 3. Get Album by ID
**Endpoint:** `GET /api/v1/panchayat/albums/{id}`  
**Purpose:** Retrieve details of a specific album

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Album ID |

#### Request Example
```bash
curl -X GET "http://localhost:8080/api/v1/panchayat/albums/1" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": 1,
    "albumName": "My Album",
    "description": "Album description",
    "coverImageUrl": "https://b2-storage-url.com/cover-image-presigned-url",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T10:30:00Z"
  }
}
```

#### Response (Error - 404 Not Found)
```json
{
  "success": false,
  "message": "Album not found",
  "data": null
}
```

---

### 4. Update Album
**Endpoint:** `PUT /api/v1/panchayat/albums/{id}`  
**Content-Type:** `multipart/form-data`  
**Purpose:** Update album details and/or cover image

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Album ID |

#### Request Parameters (Form Data)
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `albumName` | String | ❌ No | - | Updated album name |
| `description` | String | ❌ No | - | Updated description |
| `coverImageFile` | File | ❌ No | null | New cover image file |
| `compressionQuality` | String | ❌ No | HIGH | Compression quality for new image |
| `coverImageUrl` | String | ❌ No | null | New cover image URL |

#### Request Example (Update with new file)
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/albums/1 \
  -H "Authorization: Bearer <token>" \
  -F "albumName=Updated Album Name" \
  -F "description=Updated description" \
  -F "coverImageFile=@/path/to/new-image.jpg" \
  -F "compressionQuality=MEDIUM"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Album updated successfully",
  "data": {
    "id": 1,
    "albumName": "Updated Album Name",
    "description": "Updated description",
    "coverImageUrl": "https://b2-storage-url.com/new-cover-image-url",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T11:45:00Z"
  }
}
```

---

### 5. Refresh Cover Image URL
**Endpoint:** `PATCH /api/v1/panchayat/albums/{id}/refresh-cover-image-url`  
**Purpose:** Refresh presigned URL when album cover image URL expires (403 Forbidden)

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Album ID |

#### Request Example
```bash
curl -X PATCH "http://localhost:8080/api/v1/panchayat/albums/1/refresh-cover-image-url" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Cover image URL refreshed successfully",
  "data": {
    "id": 1,
    "albumName": "My Album",
    "description": "Album description",
    "coverImageUrl": "https://b2-storage-url.com/cover-image-new-presigned-url",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T12:00:00Z"
  }
}
```

---

### 6. Add Gallery Images to Album
**Endpoint:** `POST /api/v1/panchayat/albums/{id}/images`  
**Content-Type:** `application/json`  
**Purpose:** Associate existing gallery images with an album

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Album ID |

#### Request Body (JSON)
```json
{
  "galleryImageIds": [1, 2, 3, 4]
}
```

#### Request Example
```bash
curl -X POST "http://localhost:8080/api/v1/panchayat/albums/1/images" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "galleryImageIds": [1, 2, 3]
  }'
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Successfully added 3 images to album",
  "data": {
    "albumId": 1,
    "imagesAdded": 3,
    "totalRequested": 3
  }
}
```

#### Response (Error - 400 Bad Request - No images)
```json
{
  "success": false,
  "message": "Gallery image IDs are required",
  "data": null
}
```

#### Response (Partial Success)
```json
{
  "success": true,
  "message": "Successfully added 2 images to album",
  "data": {
    "albumId": 1,
    "imagesAdded": 2,
    "totalRequested": 3
  }
}
```

---

### 7. Delete Album
**Endpoint:** `DELETE /api/v1/panchayat/albums/{id}`  
**Purpose:** Delete album and its cover image from cloud storage. Gallery images are preserved but album association is cleared.

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Album ID |

#### Request Example
```bash
curl -X DELETE "http://localhost:8080/api/v1/panchayat/albums/1" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Album deleted successfully",
  "data": null
}
```

#### Response (Error - 404 Not Found)
```json
{
  "success": false,
  "message": "Album not found",
  "data": null
}
```

---

## Gallery Image APIs

### Base Endpoint: `/gallery`

---

### 1. Create Gallery Image
**Endpoint:** `POST /api/v1/panchayat/gallery`  
**Content-Type:** `multipart/form-data`  
**Purpose:** Upload a new gallery image with optional file or URL

#### Request Parameters (Form Data)
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `caption` | String | ❌ No | null | Image caption |
| `tags` | String | ❌ No | null | Image tags (comma-separated) |
| `albumId` | Long | ❌ No | null | Album ID to associate with |
| `displayOrder` | Integer | ❌ No | null | Display order within album |
| `imageFile` | File | ❌ No | null | Image file to upload and compress |
| `compressionQuality` | String | ❌ No | HIGH | Compression quality: `HIGH`, `MEDIUM`, or `LOW` |
| `imageUrl` | String | ❌ No | null | Direct image URL (if not uploading file) |

#### Request Example (File Upload)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer <token>" \
  -F "caption=Beautiful Landscape" \
  -F "tags=nature,landscape,outdoor" \
  -F "albumId=1" \
  -F "displayOrder=1" \
  -F "imageFile=@/path/to/image.jpg" \
  -F "compressionQuality=HIGH"
```

#### Request Example (URL Only)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer <token>" \
  -F "caption=Beautiful Landscape" \
  -F "tags=nature,landscape" \
  -F "imageUrl=https://example.com/image.jpg"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "id": 1,
    "caption": "Beautiful Landscape",
    "tags": "nature,landscape,outdoor",
    "albumId": 1,
    "displayOrder": 1,
    "imageUrl": "https://b2-storage-url.com/gallery-image-presigned-url",
    "uploadedBy": "user@example.com",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T10:30:00Z"
  }
}
```

---

### 2. Get All Gallery Images
**Endpoint:** `GET /api/v1/panchayat/gallery`  
**Purpose:** Retrieve paginated list of gallery images with optional album filter

#### Request Parameters (Query)
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | ❌ No | 0 | Page number (0-indexed) |
| `size` | Integer | ❌ No | 20 | Number of images per page |
| `albumId` | Long | ❌ No | null | Filter images by album ID |

#### Request Example (All images)
```bash
curl -X GET "http://localhost:8080/api/v1/panchayat/gallery?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

#### Request Example (Filter by album)
```bash
curl -X GET "http://localhost:8080/api/v1/panchayat/gallery?page=0&size=10&albumId=1" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "caption": "Beautiful Landscape",
        "tags": "nature,landscape",
        "albumId": 1,
        "displayOrder": 1,
        "imageUrl": "https://b2-storage-url.com/...",
        "uploadedBy": "user@example.com",
        "createdAt": "2025-11-26T10:30:00Z",
        "updatedAt": "2025-11-26T10:30:00Z"
      },
      {
        "id": 2,
        "caption": "Mountain View",
        "tags": "mountain,scenic",
        "albumId": 1,
        "displayOrder": 2,
        "imageUrl": "https://b2-storage-url.com/...",
        "uploadedBy": "user@example.com",
        "createdAt": "2025-11-26T10:35:00Z",
        "updatedAt": "2025-11-26T10:35:00Z"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 25,
    "totalPages": 3,
    "isFirst": true,
    "isLast": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 3. Get Gallery Image by ID
**Endpoint:** `GET /api/v1/panchayat/gallery/{id}`  
**Purpose:** Retrieve details of a specific gallery image

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Gallery image ID |

#### Request Example
```bash
curl -X GET "http://localhost:8080/api/v1/panchayat/gallery/1" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": {
    "id": 1,
    "caption": "Beautiful Landscape",
    "tags": "nature,landscape,outdoor",
    "albumId": 1,
    "displayOrder": 1,
    "imageUrl": "https://b2-storage-url.com/gallery-image-presigned-url",
    "uploadedBy": "user@example.com",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T10:30:00Z"
  }
}
```

---

### 4. Update Gallery Image
**Endpoint:** `PUT /api/v1/panchayat/gallery/{id}`  
**Content-Type:** `multipart/form-data`  
**Purpose:** Update gallery image metadata and/or image file

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Gallery image ID |

#### Request Parameters (Form Data)
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `caption` | String | ❌ No | - | Updated caption |
| `tags` | String | ❌ No | - | Updated tags |
| `albumId` | Long | ❌ No | - | Updated album association |
| `displayOrder` | Integer | ❌ No | - | Updated display order |
| `imageFile` | File | ❌ No | null | New image file |
| `compressionQuality` | String | ❌ No | HIGH | Compression quality for new image |
| `imageUrl` | String | ❌ No | null | New image URL |

#### Request Example (Update metadata)
```bash
curl -X PUT "http://localhost:8080/api/v1/panchayat/gallery/1" \
  -H "Authorization: Bearer <token>" \
  -F "caption=Updated Caption" \
  -F "tags=updated,tags" \
  -F "displayOrder=2"
```

#### Request Example (Update with new image)
```bash
curl -X PUT "http://localhost:8080/api/v1/panchayat/gallery/1" \
  -H "Authorization: Bearer <token>" \
  -F "caption=Updated Caption" \
  -F "imageFile=@/path/to/new-image.jpg" \
  -F "compressionQuality=MEDIUM"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Image updated successfully",
  "data": {
    "id": 1,
    "caption": "Updated Caption",
    "tags": "updated,tags",
    "albumId": 1,
    "displayOrder": 2,
    "imageUrl": "https://b2-storage-url.com/updated-image-url",
    "uploadedBy": "user@example.com",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T11:45:00Z"
  }
}
```

---

### 5. Refresh Gallery Image URL
**Endpoint:** `PATCH /api/v1/panchayat/gallery/{id}/refresh-image-url`  
**Purpose:** Refresh presigned URL when gallery image URL expires (403 Forbidden)

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Gallery image ID |

#### Request Example
```bash
curl -X PATCH "http://localhost:8080/api/v1/panchayat/gallery/1/refresh-image-url" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Image URL refreshed successfully",
  "data": {
    "id": 1,
    "caption": "Beautiful Landscape",
    "tags": "nature,landscape,outdoor",
    "albumId": 1,
    "displayOrder": 1,
    "imageUrl": "https://b2-storage-url.com/gallery-image-new-presigned-url",
    "uploadedBy": "user@example.com",
    "createdAt": "2025-11-26T10:30:00Z",
    "updatedAt": "2025-11-26T12:00:00Z"
  }
}
```

---

### 6. Delete Gallery Image
**Endpoint:** `DELETE /api/v1/panchayat/gallery/{id}`  
**Purpose:** Delete gallery image and associated image from cloud storage

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | Long | ✅ Yes | Gallery image ID |

#### Request Example
```bash
curl -X DELETE "http://localhost:8080/api/v1/panchayat/gallery/1" \
  -H "Authorization: Bearer <token>"
```

#### Response (Success - 200 OK)
```json
{
  "success": true,
  "message": "Image deleted successfully",
  "data": null
}
```

---

## Response Format

### Success Response
```json
{
  "success": true,
  "message": "Operation message",
  "data": {
    // Response data object or null
  }
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

---

## Common Parameters

### Compression Quality Options
- **HIGH** - Best quality, larger file size (default)
- **MEDIUM** - Balanced quality and file size
- **LOW** - Lower quality, smallest file size

### HTTP Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad Request (invalid parameters) |
| 403 | Forbidden (unauthorized/insufficient permissions) |
| 404 | Not Found (resource doesn't exist) |
| 500 | Internal Server Error |

### Authentication
All endpoints require Bearer token in Authorization header:
```
Authorization: Bearer <jwt-token>
```

### Pagination Response
- `pageNumber` - Current page (0-indexed)
- `pageSize` - Number of items per page
- `totalElements` - Total number of items
- `totalPages` - Total number of pages
- `isFirst` - Is this the first page?
- `isLast` - Is this the last page?
- `hasNext` - Does a next page exist?
- `hasPrevious` - Does a previous page exist?

---

## API Usage Workflow Examples

### Example 1: Create Album with Cover Image
1. **POST** `/albums` - Upload album with cover image
2. Receive `albumId` and `coverImageUrl`

### Example 2: Add Gallery Images to Album
1. **POST** `/gallery` - Upload gallery images
2. Receive gallery image IDs
3. **POST** `/albums/{albumId}/images` - Associate images with album

### Example 3: Refresh Expired URLs
1. **GET** `/albums/{id}` or `/gallery/{id}` - Get resource
2. If image URL returns 403 Forbidden
3. **PATCH** `/albums/{id}/refresh-cover-image-url` or `/gallery/{id}/refresh-image-url`
4. Receive updated resource with fresh presigned URL

### Example 4: Update Album with New Cover
1. **PUT** `/albums/{id}` - Upload new cover image
2. Old image is automatically deleted
3. Receive updated album with new image URL

---

## Error Handling Guide

| Scenario | Status | Message |
|----------|--------|---------|
| Missing required parameter | 400 | "[Parameter] is required" |
| Invalid compression quality | 400 | "Invalid compression quality" |
| Album not found | 404 | "Album not found" |
| Gallery image not found | 404 | "Gallery image not found" |
| No images to add | 400 | "Gallery image IDs are required" |
| Unauthorized access | 403 | "Forbidden" |
| Server error | 500 | "Internal server error" |


