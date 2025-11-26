# Image Management Implementation for Gallery & Album Sections

## Overview
This document summarizes the image management implementation for Gallery and Album sections, following the established patterns from the Post section.

---

## Implementation Complete ✅

### 1. **DTOs Updated**

#### GalleryImageRequestDTO
- **Added**: `MultipartFile imageFile` for file uploads
- **Added**: `String compressionQuality` (HIGH, MEDIUM, LOW)
- **Kept**: `String imageUrl` for backward compatibility
- **Purpose**: Support both file upload and URL-only workflows

#### AlbumRequestDTO
- **Added**: `MultipartFile coverImageFile` for cover image uploads
- **Added**: `String compressionQuality` (HIGH, MEDIUM, LOW)
- **Kept**: `String coverImageUrl` for backward compatibility
- **Purpose**: Support both file upload and URL-only workflows

---

### 2. **Controllers Updated**

#### PanchayatGalleryController
**New Methods:**
- `POST /api/v1/panchayat/gallery` - Create gallery image with optional file upload
  - Consumes: `MULTIPART_FORM_DATA_VALUE`
  - Parameters: caption, tags, albumId, displayOrder, imageFile, compressionQuality, imageUrl
  - Returns: Created gallery image with presigned URL from B2
  
- `PUT /api/v1/panchayat/gallery/{id}` - Update gallery image with optional new image
  - Consumes: `MULTIPART_FORM_DATA_VALUE`
  - Replaces old image if new file provided
  - Deletes old image from cloud storage before uploading new one
  
- `PATCH /api/v1/panchayat/gallery/{id}/refresh-image-url` - Refresh expired presigned URL
  - Call when image returns 403 Forbidden
  - Regenerates presigned URL without re-uploading
  
- `GET /api/v1/panchayat/gallery` - Get all gallery images (unchanged)
- `GET /api/v1/panchayat/gallery/{id}` - Get by ID (unchanged)
- `DELETE /api/v1/panchayat/gallery/{id}` - Delete gallery image (enhanced)
  - Now deletes associated image from cloud storage

**Features:**
- Comprehensive Javadoc for all endpoints
- Proper logging at INFO level for operations
- Error handling with graceful fallback
- Support for both multipart and JSON payloads

#### PanchayatAlbumController
**New Methods:**
- `POST /api/v1/panchayat/albums` - Create album with optional cover image upload
  - Consumes: `MULTIPART_FORM_DATA_VALUE`
  - Parameters: albumName (required), description, coverImageFile, compressionQuality, coverImageUrl
  - Returns: Created album with cover image URL
  
- `PUT /api/v1/panchayat/albums/{id}` - Update album with optional new cover image
  - Consumes: `MULTIPART_FORM_DATA_VALUE`
  - Replaces cover image if new file provided
  - Deletes old cover image from cloud storage
  
- `PATCH /api/v1/panchayat/albums/{id}/refresh-cover-image-url` - Refresh expired cover image URL
  - Call when cover image returns 403 Forbidden
  
- `GET /api/v1/panchayat/albums` - Get all albums (unchanged)
- `GET /api/v1/panchayat/albums/{id}` - Get by ID (unchanged)
- `DELETE /api/v1/panchayat/albums/{id}` - Delete album (enhanced)
  - Deletes cover image from cloud storage
  - Gallery images preserved (album association cleared)

**Features:**
- Comprehensive Javadoc for all endpoints
- Proper logging at INFO level for operations
- Error handling with graceful fallback

---

### 3. **Facades Enhanced**

#### GalleryImageFacade
**New Core Methods:**
- `create(GalleryImageRequestDTO, email)` - Creates gallery image with image processing
  - Validates image if file provided
  - Compresses to target size (~500KB)
  - Uploads compressed image to Backblaze B2
  - Stores presigned URL in database
  - **Graceful fallback**: Creates gallery without image if upload fails
  
- `update(id, GalleryImageRequestDTO)` - Updates gallery image with optional image replacement
  - If new image provided:
    - Deletes old image from B2
    - Compresses and uploads new image
    - Updates presigned URL
  - Allows updating: caption, tags, album association, displayOrder
  
- `refreshImageUrl(id)` - Regenerates presigned URL when expired
  - Extracts file key from stored URL
  - Calls `CloudStorageService.regeneratePresignedUrl()`
  - Updates database with new presigned URL
  - Throws exception if no image or storage disabled
  
- `delete(id)` - Deletes gallery image and removes from cloud storage
  - Extracts file key from URL
  - Calls `CloudStorageService.deleteImage()`
  - Continues even if cloud deletion fails (logs warning)

**Helper Methods:**
- `processAndUploadImage()` - Orchestrates compression and upload pipeline
  - Step 1: Validate image (size, type, extension)
  - Step 2: Compress to target size with specified quality
  - Step 3: Get compressed image stream
  - Step 4: Upload to B2 and get presigned URL
  - Returns null if storage disabled (allows graceful fallback)
  
- `extractFileKeyFromUrl()` - Parses B2 presigned URL
  - Format: `https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext?auth=...`
  - Returns: `images/timestamp-uuid.ext` (file key for B2)
  - Returns null for invalid formats
  
- `deleteImageFile()` - Removes image from B2
  - Extracts file key from URL
  - Calls CloudStorageService
  - Logs warning but doesn't throw (non-critical operation)
  
- `parseCompressionQuality()` - Converts string to enum
  - Defaults to HIGH if invalid
  - Case-insensitive

**Key Features:**
- Full transaction support with `@Transactional`
- Comprehensive logging (INFO for happy path, WARN for non-critical failures, ERROR for issues)
- Error handling with clear messages
- URL extraction logic for B2 presigned URLs
- Graceful degradation (continues even if image upload fails)

#### AlbumFacade
**New Core Methods:**
- `create(AlbumRequestDTO)` - Creates album with optional cover image
  - Same pattern as GalleryImageFacade
  - Processes cover image if provided
  - Graceful fallback: Creates album without cover if upload fails
  
- `update(id, AlbumRequestDTO)` - Updates album with optional cover image replacement
  - Same pattern as GalleryImageFacade
  - Replaces cover image if new file provided
  
- `refreshCoverImageUrl(id)` - Regenerates presigned URL when expired
  - Same pattern as GalleryImageFacade
  
- `delete(id)` - Deletes album and removes cover image from B2
  - Same pattern as GalleryImageFacade
  - Gallery images preserved

**Helper Methods:**
- Same helper methods as GalleryImageFacade
- Identical URL extraction and file key parsing logic
- Identical image processing pipeline

**Key Features:**
- Mirrors GalleryImageFacade implementation
- Full transaction support
- Comprehensive logging
- Error handling with graceful fallback
- URL extraction and regeneration

---

## Image Processing Pipeline

### Complete Flow (File Upload)
```
1. Controller receives multipart form data
   ↓
2. Build DTO with form parameters
   ↓
3. Facade.create/update() called
   ↓
4. Check if imageFile provided and not empty
   ↓
5. processAndUploadImage():
   a. Validate image (size, type, extension)
   b. Compress image to target size
   c. Get compressed image stream
   d. Upload to B2 via CloudStorageService
   e. Get presigned URL from response
   ↓
6. Store presigned URL in database
   ↓
7. Return response with image URL
```

### Image Deletion Flow
```
1. Controller calls delete endpoint
   ↓
2. Facade.delete() called
   ↓
3. Get entity from database
   ↓
4. If image URL exists and cloud storage enabled:
   a. Extract file key from URL
   b. Call CloudStorageService.deleteImage(fileKey)
   ↓
5. Delete entity from database
   ↓
6. Return success response
```

### URL Refresh Flow
```
1. Frontend detects 403 Forbidden on image request
   ↓
2. Call /refresh-image-url endpoint
   ↓
3. Facade.refreshImageUrl() called
   ↓
4. Extract file key from current URL
   ↓
5. Call CloudStorageService.regeneratePresignedUrl(fileKey)
   ↓
6. Update database with new presigned URL
   ↓
7. Return updated entity with fresh URL
```

---

## Code Practices Applied

### 1. **Transactions**
- All create/update/delete operations use `@Transactional`
- Ensures atomicity and consistency

### 2. **Error Handling**
- Try-catch for image processing
- Graceful fallback: Continue even if image upload fails
- Non-critical errors logged as WARN (e.g., cloud storage deletion)
- Critical errors logged as ERROR

### 3. **Logging**
- INFO: Successful operations with metadata
- WARN: Non-critical failures (e.g., image upload skipped, deletion failed)
- ERROR: Critical failures that affect operation
- All logs include context (entity ID, filenames, file keys)

### 4. **Documentation**
- Comprehensive Javadoc on all public methods
- Clear parameter descriptions
- Return value documentation
- Exception documentation

### 5. **Validation**
- File size validation (max 10MB)
- Content type validation (JPEG, PNG, GIF, WebP)
- Extension validation
- Null safety checks throughout

### 6. **Resource Cleanup**
- Always delete old image before uploading new one
- Extract file keys correctly to ensure proper deletion
- Handle missing images gracefully

### 7. **URL Management**
- Proper parsing of B2 presigned URLs
- Extraction of file keys for regeneration/deletion
- Query parameter removal from URLs
- Support for URL format: `https://f001.backblazeb2.com/file/bucket-name/path/to/file?auth=...`

### 8. **Configuration**
- Respects `cloudStorageService.isEnabled()` setting
- Skips cloud operations if disabled
- Allows graceful operation even without cloud storage

---

## Testing Checklist

### Gallery Image Operations
- [ ] Create gallery image without file (URL only)
- [ ] Create gallery image with file upload (HIGH quality)
- [ ] Create gallery image with file upload (MEDIUM quality)
- [ ] Create gallery image with file upload (LOW quality)
- [ ] Update gallery image without changing image
- [ ] Update gallery image with new file
- [ ] Delete gallery image (verifies cloud deletion)
- [ ] Refresh image URL when expired
- [ ] Get all gallery images
- [ ] Get gallery images by album filter
- [ ] Get specific gallery image

### Album Operations
- [ ] Create album without cover image (URL only)
- [ ] Create album with cover image file
- [ ] Create album with different compression qualities
- [ ] Update album without changing cover
- [ ] Update album with new cover image
- [ ] Replace cover image (verify old image deleted)
- [ ] Delete album (verify cover image deleted from cloud)
- [ ] Refresh cover image URL when expired
- [ ] Get all albums
- [ ] Get specific album

### Error Handling
- [ ] Upload file exceeding 10MB (should fail gracefully)
- [ ] Upload invalid file type (should fail gracefully)
- [ ] Refresh URL for image that doesn't exist
- [ ] Delete when cloud storage disabled (should continue)
- [ ] Invalid compression quality (should default to HIGH)

### Cloud Storage Integration
- [ ] Image successfully compressed
- [ ] Presigned URL generated and stored
- [ ] Old image deleted when replacing
- [ ] URL refresh generates new valid presigned URL
- [ ] File keys extracted correctly from URLs
- [ ] Query parameters removed from file keys

---

## API Endpoint Reference

### Gallery Image Endpoints

**Create Gallery Image**
```
POST /api/v1/panchayat/gallery
Content-Type: multipart/form-data

Parameters:
- imageFile: [file] - Optional image file
- compressionQuality: HIGH|MEDIUM|LOW (default: HIGH)
- imageUrl: string - Optional URL if not uploading file
- caption: string - Image caption
- tags: string - Image tags
- albumId: number - Album association
- displayOrder: number - Display order
```

**Update Gallery Image**
```
PUT /api/v1/panchayat/gallery/{id}
Content-Type: multipart/form-data

Parameters: (Same as create, all optional)
```

**Refresh Image URL**
```
PATCH /api/v1/panchayat/gallery/{id}/refresh-image-url
```

**Delete Gallery Image**
```
DELETE /api/v1/panchayat/gallery/{id}
```

### Album Endpoints

**Create Album**
```
POST /api/v1/panchayat/albums
Content-Type: multipart/form-data

Parameters:
- albumName: string (required)
- description: string - Album description
- coverImageFile: [file] - Optional cover image
- compressionQuality: HIGH|MEDIUM|LOW (default: HIGH)
- coverImageUrl: string - Optional URL if not uploading file
```

**Update Album**
```
PUT /api/v1/panchayat/albums/{id}
Content-Type: multipart/form-data

Parameters: (All optional)
```

**Refresh Cover Image URL**
```
PATCH /api/v1/panchayat/albums/{id}/refresh-cover-image-url
```

**Delete Album**
```
DELETE /api/v1/panchayat/albums/{id}
```

---

## Files Modified

### DTOs
1. `src/main/java/.../dto/request/GalleryImageRequestDTO.java` - Added file upload support
2. `src/main/java/.../dto/request/AlbumRequestDTO.java` - Added file upload support

### Controllers
1. `src/main/java/.../controller/panchayat/PanchayatGalleryController.java` - Multipart endpoints
2. `src/main/java/.../controller/panchayat/PanchayatAlbumController.java` - Multipart endpoints

### Facades
1. `src/main/java/.../facade/GalleryImageFacade.java` - Image processing logic
2. `src/main/java/.../facade/AlbumFacade.java` - Image processing logic

### Used (No Changes Required)
1. `ImageCompressionService` - Image validation and compression
2. `CloudStorageService` - B2 upload, delete, URL refresh
3. `S3CloudStorageService` - B2 S3-compatible API integration

---

## Configuration Reference

### Application YAML Settings
```yaml
# Image compression settings
cloud:
  storage:
    image:
      max-width: 2560
      max-height: 2560
      max-size-bytes: 10485760  # 10MB
      compression-quality: HIGH
      target-size-bytes: 256000  # 256KB target

# Backblaze B2 settings
backblaze:
  b2:
    enabled: true  # Enable/disable cloud storage
```

---

## Common Issues & Solutions

### Issue: Image URL returns 403 Forbidden
**Solution**: Call the `/refresh-image-url` endpoint to regenerate presigned URL
```
PATCH /api/v1/panchayat/gallery/{id}/refresh-image-url
```

### Issue: Image upload fails but operation completes
**Solution**: This is graceful fallback behavior. Check logs:
```
log.error("Error processing image for upload", e);
```
Album/gallery is created but without image URL.

### Issue: Old image still exists after replacement
**Solution**: Verify cloud storage is enabled and file key extraction is working:
```
if (!cloudStorageService.isEnabled()) {
    log.warn("Cloud storage is disabled. Image deletion skipped");
}
```

### Issue: Wrong image deleted when replacing
**Solution**: Verify URL extraction logic extracts correct file key:
```
File URL: https://f001.backblazeb2.com/file/bucket-name/images/abc123.jpg?auth=...
File Key: images/abc123.jpg (extracted correctly)
```

---

## Performance Considerations

1. **Compression**: Images compressed to ~256KB target, reduces bandwidth
2. **Presigned URLs**: B2 generates 1-hour expiring URLs, refresh as needed
3. **Database**: Only URLs stored, not image data
4. **Cloud Storage**: All images persisted in Backblaze B2
5. **Transactions**: All operations atomic, ensures data consistency

---

## Future Enhancements

1. **Batch Operations**: Bulk upload multiple images
2. **Image Versions**: Store multiple versions of same image
3. **CDN Integration**: Add CDN caching for presigned URLs
4. **Thumbnail Generation**: Generate thumbnails for gallery previews
5. **Image Metadata**: Extract and store EXIF data
6. **Access Logging**: Track who accessed which images
7. **Image Analytics**: Track image view counts
8. **Watermarking**: Add watermarks to images

---

## Support

For issues or questions about image management implementation, refer to:
1. This implementation document
2. PostFacade for reference implementation
3. CloudStorageService documentation
4. ImageCompressionService documentation


