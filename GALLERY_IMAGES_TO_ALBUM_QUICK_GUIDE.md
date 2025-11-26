# Quick Reference: Add Gallery Images to Album

## Summary
The functionality to add gallery images to albums **IS FULLY IMPLEMENTED** with multiple approaches.

## Quick Start - Three Ways to Add Images to Albums

### 1️⃣ Add Single Image During Upload (Most Common)
```bash
POST /api/v1/panchayat/gallery
Content-Type: multipart/form-data

Parameters:
- imageFile: (binary) image file
- caption: "My caption"
- albumId: 1
- compressionQuality: HIGH

cURL Example:
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -F "imageFile=@photo.jpg" \
  -F "caption=Beautiful sunset" \
  -F "albumId=1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Response:
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "id": 100,
    "albumId": 1,
    "imageUrl": "https://...",
    "caption": "Beautiful sunset",
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

---

### 2️⃣ Move Existing Image to Different Album
```bash
PUT /api/v1/panchayat/gallery/{imageId}
Content-Type: multipart/form-data

Parameters:
- albumId: 2  (new album)

cURL Example:
curl -X PUT http://localhost:8080/api/v1/panchayat/gallery/100 \
  -F "albumId=2" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 3️⃣ Bulk Add Multiple Existing Images to Album (NEW) ⭐
```bash
POST /api/v1/panchayat/albums/{albumId}/images
Content-Type: application/json

Request Body:
{
  "galleryImageIds": [10, 11, 12, 13, 14]
}

cURL Example:
curl -X POST http://localhost:8080/api/v1/panchayat/albums/1/images \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "galleryImageIds": [10, 11, 12, 13, 14]
  }'
```

Response:
```json
{
  "success": true,
  "message": "Successfully added 5 images to album",
  "data": {
    "albumId": 1,
    "imagesAdded": 5,
    "totalRequested": 5
  }
}
```

---

## Common Operations

### Get All Images in an Album
```bash
GET /api/v1/panchayat/gallery?albumId=1&page=0&size=20
```

### Remove Image from Album
```bash
PUT /api/v1/panchayat/gallery/{imageId}
-F "albumId=" (leave empty/null)
```

### Delete Album (Images Remain)
```bash
DELETE /api/v1/panchayat/albums/{albumId}
```

### Update Album Details
```bash
PUT /api/v1/panchayat/albums/{albumId}
-F "albumName=New Name"
-F "description=New Description"
```

---

## Key Features

✅ **Multiple Ways to Associate Images**
- Individual upload with album
- Batch/bulk add to existing album
- Move between albums

✅ **Security**
- PANCHAYAT_ADMIN role required
- Tenant-isolated operations
- Secure presigned URLs for cloud storage

✅ **Image Management**
- Automatic image compression
- Cloud storage integration (Backblaze B2)
- Presigned URL generation and refresh

✅ **Error Handling**
- Graceful error handling
- Continue on individual failures
- Detailed logging

---

## API Endpoints Summary

| Method | Endpoint | Purpose | Role |
|--------|----------|---------|------|
| POST | `/api/v1/panchayat/albums` | Create album with cover image | PANCHAYAT_ADMIN |
| GET | `/api/v1/panchayat/albums` | List all albums | PANCHAYAT_ADMIN |
| GET | `/api/v1/panchayat/albums/{id}` | Get album details | PANCHAYAT_ADMIN |
| PUT | `/api/v1/panchayat/albums/{id}` | Update album | PANCHAYAT_ADMIN |
| POST | `/api/v1/panchayat/albums/{id}/images` | **Bulk add images** | PANCHAYAT_ADMIN |
| DELETE | `/api/v1/panchayat/albums/{id}` | Delete album | PANCHAYAT_ADMIN |
| POST | `/api/v1/panchayat/gallery` | Create/upload gallery image | PANCHAYAT_ADMIN |
| GET | `/api/v1/panchayat/gallery` | List images (with album filter) | PANCHAYAT_ADMIN |
| GET | `/api/v1/panchayat/gallery/{id}` | Get image details | PANCHAYAT_ADMIN |
| PUT | `/api/v1/panchayat/gallery/{id}` | Update image / change album | PANCHAYAT_ADMIN |
| DELETE | `/api/v1/panchayat/gallery/{id}` | Delete image | PANCHAYAT_ADMIN |

---

## Database Schema

```sql
-- Albums Table
CREATE TABLE albums (
  id BIGINT PRIMARY KEY,
  panchayat_id BIGINT NOT NULL,
  album_name VARCHAR(200) NOT NULL,
  description TEXT,
  cover_image_url VARCHAR(500),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- Gallery Images Table
CREATE TABLE gallery_images (
  id BIGINT PRIMARY KEY,
  panchayat_id BIGINT NOT NULL,
  album_id BIGINT,  -- Foreign key to albums (nullable)
  uploaded_by_user_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  caption VARCHAR(500),
  tags VARCHAR(500),
  display_order INT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (album_id) REFERENCES albums(id)
);
```

---

## Status: ✅ READY FOR PRODUCTION

### Implementation Complete
- ✅ Backend API endpoints implemented
- ✅ Database relationships configured
- ✅ Security (role-based access) enforced
- ✅ Error handling and validation
- ✅ Cloud storage integration
- ✅ Comprehensive logging
- ✅ Transaction management

### Next Steps (Optional Frontend)
- [ ] Create album UI
- [ ] Upload images with album selection
- [ ] Bulk image upload to album
- [ ] Gallery view by album
- [ ] Drag-and-drop reordering

---

## Notes

- Gallery images can exist **without** an album (album_id is nullable)
- Deleting an album **does NOT** delete gallery images (only clears association)
- Gallery images are sorted by `displayOrder` then by creation date
- Each panchayat can have unlimited albums and images
- Images are compressed before upload to cloud storage
- Presigned URLs can be refreshed when expired (403 response)

