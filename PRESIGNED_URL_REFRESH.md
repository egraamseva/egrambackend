# Presigned URL Refresh Implementation

## Overview
This implementation adds automatic presigned URL refresh functionality for expired image URLs. After a presigned URL expires (24 hours by default), the UI can request a new URL without having to re-upload the image.

## Problem Solved
- **Before**: Once a presigned URL expired after 24 hours, users would see 403 Forbidden errors when accessing images
- **After**: UI can request a fresh presigned URL on-demand, and the image continues to be accessible

## Implementation Details

### 1. **S3CloudStorageService** (`service/S3CloudStorageService.java`)
Added method: `regeneratePresignedUrl(String fileKey)`
```java
/**
 * Regenerate presigned URL for existing file when current URL expires
 * Use this when the presigned URL has expired or needs to be refreshed
 * @param fileKey S3 key/path of file
 * @return ImageCompressionDTO with newly generated presigned URL
 */
public ImageCompressionDTO regeneratePresignedUrl(String fileKey)
```
- Generates a new presigned URL (valid for 24 hours) for an existing file
- Returns the fresh URL wrapped in an ImageCompressionDTO object
- Includes comprehensive error handling and logging

### 2. **PanchayatPostController** (`controller/panchayat/PanchayatPostController.java`)
Added endpoint: `PATCH /api/v1/panchayat/posts/{id}/refresh-image-url`
```java
@PatchMapping("/{id}/refresh-image-url")
public ResponseEntity<ApiResponse<PostResponseDTO>> refreshImageUrl(@PathVariable Long id)
```
- REST endpoint to trigger URL refresh for a specific post
- Returns updated post with new presigned URL
- Requires PANCHAYAT_ADMIN role

### 3. **PostFacade** (`facade/PostFacade.java`)
Added methods:
- `refreshImageUrl(Long id)` - Main method to refresh URL for a post
- `extractFileKeyFromUrl(String mediaUrl)` - Helper to extract S3 key from presigned URL

```java
/**
 * Refresh/regenerate presigned URL for post image when expired
 * Extracts the file key from current URL and generates a new presigned URL
 * @param id Post ID
 * @return Updated post with fresh presigned URL
 */
@Transactional
public PostResponseDTO refreshImageUrl(Long id)
```

## How It Works

### URL Expiration Flow
1. Image uploaded → Presigned URL generated (valid 24 hours)
2. URL stored in Post entity in database
3. After 24 hours → URL expires, UI receives 403 error when accessing image
4. UI calls `/api/v1/panchayat/posts/{id}/refresh-image-url`
5. Backend extracts file key from expired URL
6. Backend generates new presigned URL from same file
7. New URL stored in database and returned to UI
8. UI can now access image with fresh URL

### File Key Extraction
The implementation extracts the S3 file key from presigned URLs:
- **URL Format**: `https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext?query-params`
- **Extracted Key**: `images/timestamp-uuid.ext`
- Automatically removes query parameters from presigned URLs

## UI Integration Strategies

### Option 1: Proactive Refresh (Recommended)
```javascript
// Store URL creation timestamp when image loads
// Refresh URL after 12 hours (before expiry)
if (currentTime - urlCreationTime > 12 * 60 * 60 * 1000) {
  // Call refresh endpoint
  fetch(`/api/v1/panchayat/posts/${postId}/refresh-image-url`, { method: 'PATCH' })
  .then(response => response.json())
  .then(data => {
    // Update image src with new URL
    imageElement.src = data.data.mediaUrl;
  });
}
```

### Option 2: Reactive Refresh (On 403)
```javascript
// Add error handler to image element
imageElement.onerror = () => {
  // Check if 403 error (expired URL)
  fetch(`/api/v1/panchayat/posts/${postId}/refresh-image-url`, { method: 'PATCH' })
  .then(response => response.json())
  .then(data => {
    // Update image src with fresh URL and retry load
    imageElement.src = data.data.mediaUrl;
  });
};
```

## Error Handling
- Validates that cloud storage is enabled
- Validates that post has a mediaUrl
- Properly extracts file key from URL with fallback checks
- Returns meaningful error messages
- Logs all operations for debugging

## Configuration
Presigned URL expiration time is configurable:
```yaml
backblaze:
  b2:
    presigned-url-expiration-hours: 24  # Default: 24 hours
```

## Testing Endpoints

### Refresh image URL for a post:
```bash
curl -X PATCH http://localhost:8080/api/v1/panchayat/posts/1/refresh-image-url \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Response:
```json
{
  "success": true,
  "message": "Image URL refreshed successfully",
  "data": {
    "id": 1,
    "title": "Post Title",
    "bodyText": "Post content",
    "mediaUrl": "https://f001.backblazeb2.com/file/egram-service/images/NEW-URL?NEW-SIGNATURE",
    ...
  }
}
```

## Benefits
✅ Seamless image access even after URL expiration  
✅ No need to re-upload images to refresh URLs  
✅ Automatic file key extraction from any presigned URL format  
✅ Full error handling and logging  
✅ Transactional operations ensure data consistency  
✅ Flexible UI integration (proactive or reactive)  
✅ Production-ready implementation with comprehensive comments  

## Files Modified
1. `S3CloudStorageService.java` - Added `regeneratePresignedUrl()` method
2. `PanchayatPostController.java` - Added `/refresh-image-url` endpoint
3. `PostFacade.java` - Added `refreshImageUrl()` and `extractFileKeyFromUrl()` methods

