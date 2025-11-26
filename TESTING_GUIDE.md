# Testing Guide: Image Management for Gallery & Album

## Overview
This guide provides comprehensive testing scenarios and curl examples for the new image management features in Gallery and Album sections.

---

## Prerequisites

### Requirements
- Backend server running on `http://localhost:8080`
- Authentication token (JWT Bearer token)
- Image files for testing (various sizes and formats)
- cURL or Postman for API testing

### Test Images
Create test images of varying sizes:
- `small.jpg` - 100KB
- `medium.png` - 500KB
- `large.jpg` - 2MB
- `invalid.txt` - Text file (for error testing)

---

## 1. Gallery Image Tests

### 1.1 Create Gallery Image with File Upload

#### Test Case: Upload new gallery image (HIGH quality)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@/path/to/medium.png" \
  -F "compressionQuality=HIGH" \
  -F "caption=Beautiful sunset at the panchayat" \
  -F "tags=nature,sunset" \
  -F "displayOrder=1"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "imageId": 1,
    "imageUrl": "https://f001.backblazeb2.com/file/bucket-name/images/xxx.jpg?authorization=xxx",
    "caption": "Beautiful sunset at the panchayat",
    "tags": "nature,sunset",
    "displayOrder": 1,
    "createdAt": "2025-11-26T10:30:00",
    "updatedAt": "2025-11-26T10:30:00"
  }
}
```

#### Test Case: Upload with MEDIUM compression quality
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@/path/to/large.jpg" \
  -F "compressionQuality=MEDIUM" \
  -F "caption=Test image" \
  -F "albumId=1"
```

#### Test Case: Upload with LOW compression quality
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@/path/to/medium.png" \
  -F "compressionQuality=LOW" \
  -F "caption=Low quality test"
```

#### Test Case: URL-only workflow (no file upload)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageUrl=https://example.com/image.jpg" \
  -F "caption=External image" \
  -F "tags=external"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "imageId": 2,
    "imageUrl": "https://example.com/image.jpg",
    "caption": "External image",
    "tags": "external"
  }
}
```

#### Test Case: Invalid file type (should fail gracefully)
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@/path/to/invalid.txt" \
  -F "caption=Invalid file"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "imageId": 3,
    "imageUrl": null,
    "caption": "Invalid file"
  }
}
```
**Note:** Graceful fallback - gallery image created without URL

### 1.2 Get Gallery Images

#### Test Case: Get all gallery images
```bash
curl -X GET http://localhost:8080/api/v1/panchayat/gallery?page=0&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "imageId": 1,
        "imageUrl": "https://f001.backblazeb2.com/file/...",
        "caption": "Beautiful sunset",
        "tags": "nature,sunset",
        "displayOrder": 1,
        "createdAt": "2025-11-26T10:30:00"
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Test Case: Get gallery images by album
```bash
curl -X GET http://localhost:8080/api/v1/panchayat/gallery?page=0&size=20&albumId=1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Test Case: Get specific gallery image
```bash
curl -X GET http://localhost:8080/api/v1/panchayat/gallery/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 1.3 Update Gallery Image

#### Test Case: Update metadata only
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/gallery/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "caption=Updated caption" \
  -F "tags=updated,tags" \
  -F "displayOrder=2"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Image updated successfully",
  "data": {
    "imageId": 1,
    "imageUrl": "https://f001.backblazeb2.com/file/...",
    "caption": "Updated caption",
    "tags": "updated,tags",
    "displayOrder": 2
  }
}
```

#### Test Case: Replace image file
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/gallery/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@/path/to/new-image.jpg" \
  -F "compressionQuality=HIGH"
```

**Expected Response:**
- Old image deleted from B2
- New image compressed and uploaded
- Presigned URL updated in database
- Gallery image returned with new URL

#### Test Case: Update with URL
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/gallery/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageUrl=https://new-domain.com/image.jpg"
```

### 1.4 Refresh Image URL

#### Test Case: Refresh expired presigned URL
```bash
curl -X PATCH http://localhost:8080/api/v1/panchayat/gallery/1/refresh-image-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Image URL refreshed successfully",
  "data": {
    "imageId": 1,
    "imageUrl": "https://f001.backblazeb2.com/file/...?authorization=NEW_TOKEN",
    "caption": "Beautiful sunset"
  }
}
```

#### Test Case: Refresh non-existent image (should fail)
```bash
curl -X PATCH http://localhost:8080/api/v1/panchayat/gallery/999/refresh-image-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": false,
  "message": "Gallery image with ID 999 not found"
}
```

### 1.5 Delete Gallery Image

#### Test Case: Delete gallery image
```bash
curl -X DELETE http://localhost:8080/api/v1/panchayat/gallery/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Image deleted successfully",
  "data": null
}
```

**Verification:**
- Gallery image record deleted from DB
- Image file deleted from B2 cloud storage
- Logs show file key extraction and deletion

---

## 2. Album Tests

### 2.1 Create Album

#### Test Case: Create album with cover image file
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "albumName=Community Events 2025" \
  -F "description=Photos from community events throughout 2025" \
  -F "coverImageFile=@/path/to/cover.jpg" \
  -F "compressionQuality=HIGH"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Album created successfully",
  "data": {
    "albumId": 1,
    "albumName": "Community Events 2025",
    "description": "Photos from community events throughout 2025",
    "coverImageUrl": "https://f001.backblazeb2.com/file/...",
    "imageCount": 0,
    "createdAt": "2025-11-26T10:30:00"
  }
}
```

#### Test Case: Create album with URL cover image
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "albumName=Festival 2025" \
  -F "description=Annual festival photos" \
  -F "coverImageUrl=https://example.com/cover.jpg"
```

#### Test Case: Create album without cover image
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "albumName=Work in Progress" \
  -F "description=Album without cover"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Album created successfully",
  "data": {
    "albumId": 3,
    "albumName": "Work in Progress",
    "coverImageUrl": null,
    "imageCount": 0
  }
}
```

### 2.2 Get Albums

#### Test Case: Get all albums
```bash
curl -X GET http://localhost:8080/api/v1/panchayat/albums?page=0&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Test Case: Get specific album
```bash
curl -X GET http://localhost:8080/api/v1/panchayat/albums/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2.3 Update Album

#### Test Case: Update metadata only
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/albums/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "albumName=Updated Album Name" \
  -F "description=Updated description"
```

#### Test Case: Replace cover image
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/albums/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "coverImageFile=@/path/to/new-cover.jpg" \
  -F "compressionQuality=MEDIUM"
```

**Expected Behavior:**
- Old cover image deleted from B2
- New cover image compressed and uploaded
- Album updated with new cover URL

#### Test Case: Update with cover URL
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/albums/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "coverImageUrl=https://new-domain.com/cover.jpg"
```

### 2.4 Refresh Cover Image URL

#### Test Case: Refresh expired cover image URL
```bash
curl -X PATCH http://localhost:8080/api/v1/panchayat/albums/1/refresh-cover-image-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Cover image URL refreshed successfully",
  "data": {
    "albumId": 1,
    "albumName": "Community Events 2025",
    "coverImageUrl": "https://f001.backblazeb2.com/file/...?authorization=NEW_TOKEN",
    "imageCount": 5
  }
}
```

### 2.5 Delete Album

#### Test Case: Delete album with cover image
```bash
curl -X DELETE http://localhost:8080/api/v1/panchayat/albums/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Album deleted successfully",
  "data": null
}
```

**Verification:**
- Album record deleted from DB
- Cover image deleted from B2
- Gallery images in album preserved (album association cleared)

---

## 3. Error Handling Tests

### 3.1 File Validation Errors

#### Test Case: File too large (> 10MB)
```bash
# Create 15MB test file
dd if=/dev/zero of=large-file.bin bs=1M count=15

curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@large-file.bin" \
  -F "caption=Large file"
```

**Expected Behavior:**
- Error logged: "Image file size exceeds maximum limit of 10MB"
- Gallery image created without URL (graceful fallback)
- Response HTTP 200 with null imageUrl

#### Test Case: Invalid file type
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@document.pdf" \
  -F "caption=PDF file"
```

**Expected Behavior:**
- Error logged: "Invalid image format. Allowed formats: JPEG, PNG, GIF, WebP"
- Gallery image created without URL

#### Test Case: Invalid compression quality
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@test.jpg" \
  -F "compressionQuality=INVALID" \
  -F "caption=Test"
```

**Expected Behavior:**
- Warning logged: "Invalid compression quality: INVALID, using HIGH"
- File compressed with HIGH quality
- Image uploaded successfully

### 3.2 Cloud Storage Errors

#### Test Case: Cloud storage disabled
1. Set `backblaze.b2.enabled: false` in application.yaml
2. Restart backend
3. Upload image:

```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@test.jpg"
```

**Expected Behavior:**
- Warning logged: "Cloud storage is disabled. Image upload skipped"
- Gallery image created with null imageUrl
- Operation completes successfully

### 3.3 URL Refresh Errors

#### Test Case: Refresh without image
```bash
# Create album without cover image
curl -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "albumName=No Cover"

# Try to refresh (ID should be the created album ID)
curl -X PATCH http://localhost:8080/api/v1/panchayat/albums/1/refresh-cover-image-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "success": false,
  "message": "Album has no cover image to refresh"
}
```

#### Test Case: Invalid URL format
**Manual Test:** Directly edit database to set malformed imageUrl, then call refresh

**Expected Behavior:**
- Error logged: "Cannot extract file key from URL: <malformed_url>"
- Response: "Cannot extract file key from URL"

---

## 4. Integration Tests

### 4.1 Complete Gallery Workflow

**Step 1: Create Album**
```bash
ALBUM_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/panchayat/albums \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "albumName=Test Album" \
  -F "coverImageFile=@cover.jpg" \
  -F "compressionQuality=HIGH")

ALBUM_ID=$(echo $ALBUM_RESPONSE | jq '.data.albumId')
echo "Created Album ID: $ALBUM_ID"
```

**Step 2: Create Gallery Images**
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@photo1.jpg" \
  -F "compressionQuality=HIGH" \
  -F "caption=First photo" \
  -F "albumId=$ALBUM_ID" \
  -F "displayOrder=1"
```

**Step 3: List Album Images**
```bash
curl -X GET "http://localhost:8080/api/v1/panchayat/gallery?albumId=$ALBUM_ID" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Step 4: Refresh Image URL**
```bash
curl -X PATCH http://localhost:8080/api/v1/panchayat/gallery/1/refresh-image-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Step 5: Delete Gallery Image**
```bash
curl -X DELETE http://localhost:8080/api/v1/panchayat/gallery/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Step 6: Delete Album**
```bash
curl -X DELETE "http://localhost:8080/api/v1/panchayat/albums/$ALBUM_ID" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4.2 Image Replacement Workflow

**Step 1: Create with Image**
```bash
curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@old-image.jpg" \
  -F "caption=Original image"
```

**Step 2: Get Image URL**
```bash
GALLERY_ID=1  # from previous response
curl -X GET http://localhost:8080/api/v1/panchayat/gallery/$GALLERY_ID \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data.imageUrl'
```

**Step 3: Replace Image**
```bash
curl -X PUT http://localhost:8080/api/v1/panchayat/gallery/$GALLERY_ID \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@new-image.jpg" \
  -F "compressionQuality=MEDIUM"
```

**Verification:**
- Old image deleted from B2
- New image uploaded
- URL in response should be different from step 2

---

## 5. Performance Tests

### Test Image Compression

Create test images of different sizes and measure compression:

```bash
# 1MB image
dd if=/dev/zero bs=1M count=1 | convert - test-1mb.jpg

# Compression tracking (watch logs for):
# "Image compressed successfully. Original: 1048576 bytes, Compressed: 256000 bytes"
# Compression ratio: ~75% reduction

curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "imageFile=@test-1mb.jpg" \
  -F "compressionQuality=HIGH"
```

### Test Pagination

```bash
# Create 25 gallery images
for i in {1..25}; do
  curl -X POST http://localhost:8080/api/v1/panchayat/gallery \
    -H "Authorization: Bearer YOUR_TOKEN" \
    -F "imageFile=@test.jpg" \
    -F "caption=Image $i"
done

# Get page 1 (20 items)
curl -X GET http://localhost:8080/api/v1/panchayat/gallery?page=0&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.data.content | length'

# Get page 2 (5 items)
curl -X GET http://localhost:8080/api/v1/panchayat/gallery?page=1&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN" | jq '.data.content | length'
```

---

## 6. Logging Verification

### Expected Logs After Operations

#### Successful Upload
```
[INFO] Processing image for upload: photo.jpg
[INFO] Image compressed successfully. Original: 1048576 bytes, Compressed: 256000 bytes
[INFO] Image uploaded to cloud storage. URL: https://f001.backblazeb2.com/...
[INFO] Gallery image created successfully with ID: 1
```

#### Image Replacement
```
[INFO] Deleting image from cloud storage. File Key: images/old-image-xxx.jpg
[INFO] Image deleted successfully from cloud storage
[INFO] Processing image for upload: new-image.jpg
[INFO] Image compressed successfully. Original: 512000 bytes, Compressed: 200000 bytes
[INFO] Image uploaded to cloud storage. URL: https://f001.backblazeb2.com/...
[INFO] Gallery image updated successfully with ID: 1
```

#### URL Refresh
```
[INFO] Refreshing image URL for gallery image ID: 1, file key: images/photo-xxx.jpg
[INFO] Image URL refreshed successfully for gallery image ID: 1
```

#### Deletion
```
[INFO] Deleting image from cloud storage. File Key: images/photo-xxx.jpg
[INFO] Image deleted successfully from cloud storage
[INFO] Gallery image deleted successfully with ID: 1
```

---

## Checklist: Comprehensive Testing

### Basic CRUD Operations
- [ ] Create gallery image with file upload
- [ ] Create gallery image with URL only
- [ ] Create album with cover image file
- [ ] Create album with URL only
- [ ] Read/List gallery images
- [ ] Read/List albums
- [ ] Update gallery image (metadata only)
- [ ] Update gallery image (replace file)
- [ ] Update album (metadata only)
- [ ] Update album (replace cover)
- [ ] Delete gallery image
- [ ] Delete album

### Image Processing
- [ ] File validation (size, type, extension)
- [ ] Compression (HIGH, MEDIUM, LOW qualities)
- [ ] Presigned URL generation
- [ ] File key extraction from URLs
- [ ] Query parameter removal

### URL Management
- [ ] Refresh expired gallery image URL
- [ ] Refresh expired album cover URL
- [ ] Handle missing images on refresh
- [ ] Handle cloud storage disabled

### Error Handling
- [ ] File too large (graceful fallback)
- [ ] Invalid file type (graceful fallback)
- [ ] Invalid compression quality (default to HIGH)
- [ ] Cloud storage disabled (operations complete without image)
- [ ] Missing image on refresh (proper error response)

### Cloud Storage
- [ ] Old image deleted before uploading new
- [ ] Presigned URL stored in database
- [ ] URL refresh without re-uploading
- [ ] Image fully removed when entity deleted

### Pagination
- [ ] Get first page
- [ ] Get middle pages
- [ ] Get last page
- [ ] Check total elements and pages

### Logging
- [ ] Successful operations logged at INFO
- [ ] Non-critical failures logged at WARN
- [ ] Critical errors logged at ERROR
- [ ] File keys and URLs logged appropriately


