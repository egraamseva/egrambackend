# Code Pattern Reference: Post vs Gallery vs Album

## Overview
This document shows how the Gallery and Album implementations follow the Post section's image management patterns.

---

## 1. Controller Pattern Comparison

### Post Controller (Reference)
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<PostResponseDTO>> create(
        @RequestParam String title,
        @RequestParam String bodyText,
        @RequestParam(required = false) MultipartFile imageFile,
        @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
        Authentication authentication) {
    
    String email = authentication.getName();
    PostRequestDTO request = new PostRequestDTO();
    request.setTitle(title);
    request.setBodyText(bodyText);
    request.setImageFile(imageFile);
    request.setCompressionQuality(compressionQuality.toUpperCase());
    
    PostResponseDTO response = postFacade.create(request, email);
    return ResponseEntity.ok(ApiResponse.success("Post created", response));
}
```

### Gallery Controller (Following Same Pattern)
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<GalleryImageResponseDTO>> create(
        @RequestParam(required = false) String caption,
        @RequestParam(required = false) MultipartFile imageFile,
        @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
        Authentication authentication) {
    
    String email = authentication.getName();
    GalleryImageRequestDTO request = new GalleryImageRequestDTO();
    request.setCaption(caption);
    request.setImageFile(imageFile);
    request.setCompressionQuality(compressionQuality.toUpperCase());
    
    GalleryImageResponseDTO response = galleryImageFacade.create(request, email);
    return ResponseEntity.ok(ApiResponse.success("Image uploaded", response));
}
```

### Album Controller (Following Same Pattern)
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<AlbumResponseDTO>> create(
        @RequestParam String albumName,
        @RequestParam(required = false) MultipartFile coverImageFile,
        @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality) {
    
    AlbumRequestDTO request = new AlbumRequestDTO();
    request.setAlbumName(albumName);
    request.setCoverImageFile(coverImageFile);
    request.setCompressionQuality(compressionQuality.toUpperCase());
    
    AlbumResponseDTO response = albumFacade.create(request);
    return ResponseEntity.ok(ApiResponse.success("Album created", response));
}
```

### Key Patterns Applied:
1. ✅ Use `@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)`
2. ✅ Accept `@RequestParam` for form fields
3. ✅ Accept `@RequestParam MultipartFile imageFile`
4. ✅ Accept `@RequestParam compressionQuality` with default value
5. ✅ Build DTO from form parameters in controller
6. ✅ Pass to facade for processing
7. ✅ Return `ApiResponse` with success message

---

## 2. DTO Pattern Comparison

### Post Request DTO
```java
@Data
public class PostRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String bodyText;
    
    // Image file upload option
    private MultipartFile imageFile;
    private String compressionQuality; // HIGH, MEDIUM, LOW
    
    // URL-only option (for backward compatibility)
    private String mediaUrl;
}
```

### Gallery Request DTO (Following Same Pattern)
```java
@Data
public class GalleryImageRequestDTO {
    // Image file upload option
    private MultipartFile imageFile;
    private String compressionQuality; // HIGH, MEDIUM, LOW
    
    // URL-only option (for backward compatibility)
    private String imageUrl;
    
    // Other fields
    private String caption;
    private String tags;
    private Long albumId;
    private Integer displayOrder;
}
```

### Album Request DTO (Following Same Pattern)
```java
@Data
public class AlbumRequestDTO {
    @NotBlank(message = "Album name is required")
    @Size(max = 200)
    private String albumName;
    
    private String description;
    
    // Image file upload option
    private MultipartFile coverImageFile;
    private String compressionQuality; // HIGH, MEDIUM, LOW
    
    // URL-only option (for backward compatibility)
    private String coverImageUrl;
}
```

### Key Patterns Applied:
1. ✅ Include `MultipartFile imageFile` field
2. ✅ Include `String compressionQuality` field
3. ✅ Keep URL field for backward compatibility
4. ✅ Both options can coexist
5. ✅ Use `@Data` for getter/setter generation

---

## 3. Facade Pattern Comparison

### Post Facade - Create Method (Reference)
```java
@Transactional
public PostResponseDTO create(PostRequestDTO request, String email) {
    Long tenantId = TenantContext.getTenantId();
    Panchayat panchayat = panchayatService.findById(tenantId);
    User creator = userService.findByEmail(email);

    // Handle image upload if provided
    String imageUrl = request.getMediaUrl();
    if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
        CompressionQuality quality = parseCompressionQuality(request.getCompressionQuality());
        imageUrl = processAndUploadImage(request.getImageFile(), quality);
    }

    Post post = PostTransformer.toEntity(request);
    post.setMediaUrl(imageUrl);
    post.setPanchayat(panchayat);
    post.setCreatedBy(creator);
    post.setStatus(PostStatus.DRAFT);

    post = postService.create(post);
    log.info("Post created successfully with ID: {}", post.getId());
    return PostTransformer.toDTO(post);
}
```

### Gallery Facade - Create Method (Following Same Pattern)
```java
@Transactional
public GalleryImageResponseDTO create(GalleryImageRequestDTO request, String email) {
    Long tenantId = TenantContext.getTenantId();
    Panchayat panchayat = panchayatService.findById(tenantId);
    User uploader = userService.findByEmail(email);

    // Handle image upload if provided
    String imageUrl = request.getImageUrl();
    if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
        CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
        imageUrl = processAndUploadImage(request.getImageFile(), compressionQuality);
    }

    GalleryImage image = GalleryImageTransformer.toEntity(request);
    image.setImageUrl(imageUrl);
    image.setPanchayat(panchayat);
    image.setUploadedBy(uploader);

    if (request.getAlbumId() != null) {
        Album album = albumService.findById(request.getAlbumId());
        image.setAlbum(album);
    }

    image = galleryImageService.create(image);
    log.info("Gallery image created successfully with ID: {}", image.getId());
    return GalleryImageTransformer.toDTO(image);
}
```

### Album Facade - Create Method (Following Same Pattern)
```java
@Transactional
public AlbumResponseDTO create(AlbumRequestDTO request) {
    Long tenantId = TenantContext.getTenantId();
    Panchayat panchayat = panchayatService.findById(tenantId);

    // Handle cover image upload if provided
    String coverImageUrl = request.getCoverImageUrl();
    if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
        CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
        coverImageUrl = processAndUploadImage(request.getCoverImageFile(), compressionQuality);
    }

    Album album = AlbumTransformer.toEntity(request);
    album.setCoverImageUrl(coverImageUrl);
    album.setPanchayat(panchayat);

    album = albumService.create(album);
    log.info("Album created successfully with ID: {}", album.getId());
    return AlbumTransformer.toDTO(album);
}
```

### Key Patterns Applied:
1. ✅ Use `@Transactional` for atomicity
2. ✅ Get tenant ID and entity relationships at start
3. ✅ Check if image file provided (null and isEmpty)
4. ✅ Call `processAndUploadImage()` if file provided
5. ✅ Use fallback URL if no file
6. ✅ Transform DTO to entity
7. ✅ Set relationships (panchayat, creator, album)
8. ✅ Set image URL on entity
9. ✅ Call service.create()
10. ✅ Log success with entity ID
11. ✅ Transform entity back to DTO

---

## 4. Image Processing Pipeline

### Post Facade - processAndUploadImage (Reference)
```java
private String processAndUploadImage(MultipartFile imageFile, CompressionQuality quality) {
    try {
        if (!cloudStorageService.isEnabled()) {
            log.warn("Cloud storage disabled. Image upload skipped");
            return null;
        }

        if (quality == null) {
            quality = CompressionQuality.HIGH;
        }

        log.info("Processing image for upload: {}", imageFile.getOriginalFilename());

        // Step 1: Validate image
        imageCompressionService.validateImageFile(imageFile);

        // Step 2: Compress image
        ImageCompressionDTO compressionMetadata = imageCompressionService.compressImage(
                imageFile, quality
        );

        log.info("Image compressed. Original: {} bytes, Compressed: {} bytes",
                compressionMetadata.getOriginalFileSize(),
                compressionMetadata.getCompressedFileSize());

        // Step 3: Get compressed stream
        InputStream compressedStream = imageCompressionService
                .getCompressedImageInputStream(imageFile, quality);

        // Step 4: Upload to B2
        ImageCompressionDTO uploadResult = cloudStorageService.uploadImage(
                compressedStream, compressionMetadata
        );

        log.info("Image uploaded to cloud storage. URL: {}", 
                uploadResult.getBackblazeFileUrl());
        return uploadResult.getBackblazeFileUrl();

    } catch (Exception e) {
        log.error("Error processing image for upload", e);
        // Graceful fallback: continue without image
        return null;
    }
}
```

### Gallery Facade - processAndUploadImage (Following Same Pattern)
```java
private String processAndUploadImage(MultipartFile imageFile, CompressionQuality compressionQuality) {
    try {
        if (!cloudStorageService.isEnabled()) {
            log.warn("Cloud storage is disabled. Image upload skipped");
            return null;
        }

        if (compressionQuality == null) {
            compressionQuality = CompressionQuality.HIGH;
        }

        log.info("Processing image for upload: {}", imageFile.getOriginalFilename());

        // Step 1: Validate image
        imageCompressionService.validateImageFile(imageFile);

        // Step 2: Compress image
        ImageCompressionDTO compressionMetadata = imageCompressionService.compressImage(
                imageFile, compressionQuality
        );

        log.info("Image compressed successfully. Original: {} bytes, Compressed: {} bytes",
                compressionMetadata.getOriginalFileSize(),
                compressionMetadata.getCompressedFileSize());

        // Step 3: Get compressed image stream
        InputStream compressedImageStream = imageCompressionService.getCompressedImageInputStream(
                imageFile, compressionQuality
        );

        // Step 4: Upload to Backblaze B2
        ImageCompressionDTO uploadResult = cloudStorageService.uploadImage(
                compressedImageStream, compressionMetadata
        );

        log.info("Image uploaded to cloud storage. URL: {}", 
                uploadResult.getBackblazeFileUrl());
        return uploadResult.getBackblazeFileUrl();

    } catch (Exception e) {
        log.error("Error processing image for upload", e);
        // Don't fail the entire operation if image upload fails
        return null;
    }
}
```

### Key Patterns Applied:
1. ✅ Check if cloud storage enabled
2. ✅ Default to HIGH quality if null
3. ✅ Step 1: Validate using ImageCompressionService
4. ✅ Step 2: Compress to target size
5. ✅ Log compression results
6. ✅ Step 3: Get compressed stream
7. ✅ Step 4: Upload via CloudStorageService
8. ✅ Return presigned URL from B2
9. ✅ Catch all exceptions
10. ✅ Log error but return null (graceful fallback)
11. ✅ Never throw exception (allow operation to continue)

---

## 5. Update Pattern Comparison

### Post Facade - Update (Reference)
```java
@Transactional
public PostResponseDTO update(Long id, PostRequestDTO request) {
    Post post = postService.findById(id);

    if (request.getTitle() != null) {
        post.setTitle(request.getTitle());
    }
    if (request.getBodyText() != null) {
        post.setBodyText(request.getBodyText());
    }

    // Handle image replacement
    if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
        // Delete old image if exists
        if (post.getMediaUrl() != null && cloudStorageService.isEnabled()) {
            deleteImageFile(post.getMediaUrl());
        }
        // Upload new image
        CompressionQuality quality = parseCompressionQuality(request.getCompressionQuality());
        String newImageUrl = processAndUploadImage(request.getImageFile(), quality);
        post.setMediaUrl(newImageUrl);
    } else if (request.getMediaUrl() != null) {
        // Update with URL if no new file
        post.setMediaUrl(request.getMediaUrl());
    }

    post = postService.update(post);
    log.info("Post updated successfully with ID: {}", id);
    return PostTransformer.toDTO(post);
}
```

### Gallery Facade - Update (Following Same Pattern)
```java
@Transactional
public GalleryImageResponseDTO update(Long id, GalleryImageRequestDTO request) {
    GalleryImage image = galleryImageService.findById(id);

    if (request.getCaption() != null) {
        image.setCaption(request.getCaption());
    }
    if (request.getTags() != null) {
        image.setTags(request.getTags());
    }

    // Handle image update if new image provided
    if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
        // Delete old image if exists
        if (image.getImageUrl() != null && cloudStorageService.isEnabled()) {
            deleteImageFile(image.getImageUrl());
        }
        // Upload new image
        CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
        String newImageUrl = processAndUploadImage(request.getImageFile(), compressionQuality);
        image.setImageUrl(newImageUrl);
    } else if (request.getImageUrl() != null) {
        // Update with provided URL if no new file
        image.setImageUrl(request.getImageUrl());
    }

    image = galleryImageService.update(image);
    log.info("Gallery image updated successfully with ID: {}", id);
    return GalleryImageTransformer.toDTO(image);
}
```

### Key Patterns Applied:
1. ✅ Get entity first
2. ✅ Update individual fields only if provided (null check)
3. ✅ For image replacement:
   - Check if new file provided
   - Delete old image if exists
   - Upload new image
   - Update image URL
4. ✅ For URL-only update:
   - If file not provided but URL is, use URL
5. ✅ Call service.update()
6. ✅ Log success with ID
7. ✅ Transform to DTO before returning

---

## 6. Delete Pattern Comparison

### Post Facade - Delete (Reference)
```java
@Transactional
public void delete(Long id) {
    Post post = postService.findById(id);

    // Delete image from cloud storage if exists
    if (post.getMediaUrl() != null && cloudStorageService.isEnabled()) {
        deleteImageFile(post.getMediaUrl());
    }

    postService.delete(id);
    log.info("Post deleted successfully with ID: {}", id);
}
```

### Gallery Facade - Delete (Following Same Pattern)
```java
@Transactional
public void delete(Long id) {
    GalleryImage image = galleryImageService.findById(id);

    // Delete image from cloud storage if exists
    if (image.getImageUrl() != null && cloudStorageService.isEnabled()) {
        deleteImageFile(image.getImageUrl());
    }

    galleryImageService.delete(id);
    log.info("Gallery image deleted successfully with ID: {}", id);
}
```

### Album Facade - Delete (Following Same Pattern)
```java
@Transactional
public void delete(Long id) {
    Album album = albumService.findById(id);

    // Delete cover image from cloud storage if exists
    if (album.getCoverImageUrl() != null && cloudStorageService.isEnabled()) {
        deleteImageFile(album.getCoverImageUrl());
    }

    albumService.delete(id);
    log.info("Album deleted successfully with ID: {}", id);
}
```

### Key Patterns Applied:
1. ✅ Use `@Transactional`
2. ✅ Get entity first
3. ✅ Check if image URL exists
4. ✅ Check if cloud storage enabled
5. ✅ Call deleteImageFile() helper
6. ✅ Delete entity from DB
7. ✅ Log success with ID

---

## 7. URL Refresh Pattern (New Feature)

### Post Facade - refreshImageUrl
```java
@Transactional
public PostResponseDTO refreshImageUrl(Long id) {
    Post post = postService.findById(id);

    if (post.getMediaUrl() == null || post.getMediaUrl().isEmpty()) {
        throw new IllegalArgumentException("Post has no image to refresh");
    }

    if (!cloudStorageService.isEnabled()) {
        throw new IllegalStateException("Cloud storage is not enabled");
    }

    try {
        // Extract file key from URL
        String fileKey = extractFileKeyFromUrl(post.getMediaUrl());

        if (fileKey == null || fileKey.isEmpty()) {
            throw new IllegalArgumentException("Cannot extract file key from URL");
        }

        log.info("Refreshing image URL for post ID: {}, file key: {}", id, fileKey);

        // Generate new presigned URL
        ImageCompressionDTO refreshed = cloudStorageService.regeneratePresignedUrl(fileKey);

        if (refreshed != null && refreshed.getBackblazeFileUrl() != null) {
            post.setMediaUrl(refreshed.getBackblazeFileUrl());
            post = postService.update(post);
            log.info("Image URL refreshed successfully for post ID: {}", id);
        } else {
            throw new RuntimeException("Failed to generate new presigned URL");
        }

        return PostTransformer.toDTO(post);

    } catch (Exception e) {
        log.error("Error refreshing image URL for post ID: {}", id, e);
        throw new RuntimeException("Failed to refresh image URL: " + e.getMessage(), e);
    }
}
```

### Gallery Facade - refreshImageUrl (Following Same Pattern)
```java
@Transactional
public GalleryImageResponseDTO refreshImageUrl(Long id) {
    GalleryImage image = galleryImageService.findById(id);

    if (image.getImageUrl() == null || image.getImageUrl().isEmpty()) {
        throw new IllegalArgumentException("Gallery image has no image to refresh");
    }

    if (!cloudStorageService.isEnabled()) {
        throw new IllegalStateException("Cloud storage is not enabled");
    }

    try {
        // Extract file key from URL
        String fileKey = extractFileKeyFromUrl(image.getImageUrl());

        if (fileKey == null || fileKey.isEmpty()) {
            throw new IllegalArgumentException("Cannot extract file key from URL: " + image.getImageUrl());
        }

        log.info("Refreshing image URL for gallery image ID: {}, file key: {}", id, fileKey);

        // Generate new presigned URL
        ImageCompressionDTO refreshed = cloudStorageService.regeneratePresignedUrl(fileKey);

        if (refreshed != null && refreshed.getBackblazeFileUrl() != null) {
            image.setImageUrl(refreshed.getBackblazeFileUrl());
            image = galleryImageService.update(image);
            log.info("Image URL refreshed successfully for gallery image ID: {}", id);
        } else {
            throw new RuntimeException("Failed to generate new presigned URL");
        }

        return GalleryImageTransformer.toDTO(image);

    } catch (Exception e) {
        log.error("Error refreshing image URL for gallery image ID: {}", id, e);
        throw new RuntimeException("Failed to refresh image URL: " + e.getMessage(), e);
    }
}
```

### Key Patterns Applied:
1. ✅ Use `@Transactional`
2. ✅ Get entity first
3. ✅ Validate entity has image URL
4. ✅ Check if cloud storage enabled
5. ✅ Extract file key from current URL
6. ✅ Call CloudStorageService.regeneratePresignedUrl()
7. ✅ Update entity with new URL
8. ✅ Log success with ID and file key
9. ✅ Throw exceptions for error cases
10. ✅ Transform to DTO before returning

---

## 8. Helper Methods Pattern

### File Key Extraction
```java
private String extractFileKeyFromUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isEmpty()) {
        return null;
    }

    try {
        int fileIndex = imageUrl.indexOf("/file/");
        if (fileIndex == -1) {
            log.warn("Invalid image URL format: {}", imageUrl);
            return null;
        }

        int bucketStart = fileIndex + 6;
        int bucketEnd = imageUrl.indexOf("/", bucketStart);
        if (bucketEnd == -1) {
            log.warn("Cannot extract bucket from URL: {}", imageUrl);
            return null;
        }

        // Extract file key (everything after bucket-name/)
        String fileKey = imageUrl.substring(bucketEnd + 1);

        // Remove query parameters if present
        int queryIndex = fileKey.indexOf("?");
        if (queryIndex != -1) {
            fileKey = fileKey.substring(0, queryIndex);
        }

        return fileKey;

    } catch (Exception e) {
        log.error("Error extracting file key from URL: {}", imageUrl, e);
        return null;
    }
}
```

### File Deletion
```java
private void deleteImageFile(String imageUrl) {
    try {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        // Extract file key and delete
        int fileIndex = imageUrl.indexOf("/file/");
        if (fileIndex == -1) {
            log.warn("Invalid image URL format: {}", imageUrl);
            return;
        }

        int bucketStart = fileIndex + 6;
        int bucketEnd = imageUrl.indexOf("/", bucketStart);
        String fileKey = imageUrl.substring(bucketEnd + 1);

        // Remove query parameters
        int queryIndex = fileKey.indexOf("?");
        if (queryIndex != -1) {
            fileKey = fileKey.substring(0, queryIndex);
        }

        if (!fileKey.isEmpty()) {
            log.info("Deleting image from cloud storage. File Key: {}", fileKey);
            cloudStorageService.deleteImage(fileKey);
        }
    } catch (Exception e) {
        log.warn("Failed to delete image from cloud storage. URL: {}", imageUrl, e);
        // Don't throw - continue operation
    }
}
```

### Compression Quality Parsing
```java
private CompressionQuality parseCompressionQuality(String qualityString) {
    if (qualityString == null || qualityString.isEmpty()) {
        return CompressionQuality.HIGH;
    }

    try {
        return CompressionQuality.valueOf(qualityString.toUpperCase());
    } catch (IllegalArgumentException e) {
        log.warn("Invalid compression quality: {}, using HIGH", qualityString);
        return CompressionQuality.HIGH;
    }
}
```

### Key Patterns Applied:
1. ✅ Null/empty checks
2. ✅ Try-catch for safety
3. ✅ String parsing for file key extraction
4. ✅ Query parameter removal
5. ✅ Appropriate logging levels
6. ✅ Return null on error (for extraction)
7. ✅ Don't throw on deletion (non-critical)

---

## Summary of Code Practices

### Architecture
- **Facade Pattern**: Business logic centralized, controllers thin
- **DTO Pattern**: Data transformation layer
- **Service Pattern**: Data access and basic CRUD
- **Transformer Pattern**: Entity ↔ DTO conversion

### Transactions
- `@Transactional` on all create/update/delete
- Ensures atomicity across DB and cloud storage

### Error Handling
- Image upload failures: Graceful fallback (return null)
- Image deletion failures: Log warning, don't throw
- URL refresh failures: Throw exception (critical)

### Logging
- INFO: Successful operations with details
- WARN: Non-critical failures
- ERROR: Critical failures that propagate

### URL Management
- Extract file keys: Parse presigned URLs
- Remove query parameters: Ensure clean file keys
- Support B2 format: `https://f001.backblazeb2.com/file/bucket/path`

### Cloud Storage
- Check enabled status: Skip operations if disabled
- Proper cleanup: Delete old before uploading new
- Presigned URLs: 1-hour expiry, refresh as needed


