package in.gram.gov.app.egram_service.controller;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.domain.entity.Album;
import in.gram.gov.app.egram_service.domain.entity.GalleryImage;
import in.gram.gov.app.egram_service.domain.entity.Post;
import in.gram.gov.app.egram_service.service.S3CloudStorageService;
import in.gram.gov.app.egram_service.service.PostService;
import in.gram.gov.app.egram_service.service.GalleryImageService;
import in.gram.gov.app.egram_service.service.AlbumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * File Controller
 * Handles generic file operations including presigned URL refresh
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final S3CloudStorageService s3CloudStorageService;
    private final PostService postService;
    private final GalleryImageService galleryImageService;
    private final AlbumService albumService;

    /**
     * Refresh presigned URL for a file
     * This endpoint can be used for any file (gallery images, cover images, attachments, etc.)
     * Optionally updates the database with the new presigned URL for fault tolerance
     * 
     * @param fileKey The S3 file key/path (e.g., "images/timestamp-uuid.ext")
     *                Can also accept a full presigned URL - will extract the key automatically
     * @param entityType Optional: Type of entity to update (post, gallery, album, newsletter)
     * @param entityId Optional: ID of the entity to update
     * @return New presigned URL with expiration time
     */
    @GetMapping("/refresh-url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshPresignedUrl(
            @RequestParam String fileKey,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        
        log.info("Refreshing presigned URL for fileKey: {}, entityType: {}, entityId: {}", 
                fileKey, entityType, entityId);
        
        try {
            // Validate input
            if (fileKey == null || fileKey.trim().isEmpty()) {
                log.warn("Invalid fileKey provided: {}", fileKey);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File key is required and cannot be empty"));
            }
            
            // Check if B2 is enabled
            if (!s3CloudStorageService.isB2Enabled()) {
                log.warn("B2 is not enabled, cannot refresh presigned URL");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cloud storage is not enabled"));
            }
            
            // Extract file key from URL if a full URL is provided
            String extractedFileKey = extractFileKeyFromUrl(fileKey);
            String actualFileKey = extractedFileKey != null ? extractedFileKey : fileKey.trim();
            
            if (actualFileKey.isEmpty()) {
                log.warn("Could not extract file key from: {}", fileKey);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid file key format. Cannot extract file key from URL."));
            }
            
            log.debug("Generating new presigned URL for fileKey: {}", actualFileKey);
            
            // Generate new presigned URL with configured expiration
            String newPresignedUrl = s3CloudStorageService.getFileUrl(actualFileKey);
            
            if (newPresignedUrl == null || newPresignedUrl.isEmpty()) {
                log.error("Failed to generate presigned URL for fileKey: {}", actualFileKey);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate presigned URL. Please check if the file exists and cloud storage is properly configured."));
            }
            
            // Get actual expiration time from service (in seconds)
            int expiresInSeconds = s3CloudStorageService.getPresignedUrlExpirationSeconds();
            
            // Optionally update database with new presigned URL (fault-tolerant)
            if (entityType != null && entityId != null) {
                try {
                    updateEntityUrlInDatabase(entityType, entityId, newPresignedUrl, actualFileKey);
                } catch (Exception dbError) {
                    // Log but don't fail - URL refresh succeeded
                    log.warn("Database update failed for {} (ID: {}), but URL refresh succeeded: {}", 
                            entityType, entityId, dbError.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileKey", actualFileKey);
            response.put("presignedUrl", newPresignedUrl);
            response.put("expiresIn", expiresInSeconds);
            
            log.info("Presigned URL refreshed successfully for fileKey: {} (expires in {} seconds)", 
                    actualFileKey, expiresInSeconds);
            return ResponseEntity.ok(ApiResponse.success("Presigned URL refreshed successfully", response));
            
        } catch (Exception e) {
            log.error("Error refreshing presigned URL for fileKey: {}", fileKey, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to refresh presigned URL: " + e.getMessage()));
        }
    }

    /**
     * Update entity URL in database (fault-tolerant - won't fail if update fails)
     * 
     * @param entityType Type of entity (post, gallery, album, newsletter)
     * @param entityId ID of the entity
     * @param newPresignedUrl New presigned URL to store
     * @param fileKey File key for validation
     */
    @Transactional
    protected void updateEntityUrlInDatabase(String entityType, Long entityId, String newPresignedUrl, String fileKey) {
        try {
            switch (entityType.toLowerCase()) {
                case "post":
                    updatePostUrl(entityId, newPresignedUrl, fileKey);
                    break;
                case "gallery":
                case "galleryimage":
                    updateGalleryImageUrl(entityId, newPresignedUrl, fileKey);
                    break;
                case "album":
                    updateAlbumUrl(entityId, newPresignedUrl, fileKey);
                    break;
                case "newsletter":
                    // Newsletter uses coverImageFileKey, so we don't update URL directly
                    // The fileKey is already stored, URL is generated on-the-fly
                    log.debug("Newsletter entity doesn't store presigned URL, skipping update");
                    break;
                default:
                    log.warn("Unknown entity type for URL update: {}", entityType);
            }
        } catch (Exception e) {
            // Fault-tolerant: log error but don't fail the request
            log.error("Failed to update {} entity (ID: {}) with new presigned URL. " +
                    "This is non-critical - URL refresh succeeded but DB update failed: {}", 
                    entityType, entityId, e.getMessage());
            // Don't throw - allow the request to succeed with the new URL
        }
    }

    /**
     * Update post media URL in database
     */
    private void updatePostUrl(Long postId, String newPresignedUrl, String fileKey) {
        try {
            Post post = postService.findById(postId);
            // Verify the file key matches before updating
            String existingFileKey = extractFileKeyFromUrl(post.getMediaUrl());
            if (existingFileKey != null && existingFileKey.equals(fileKey)) {
                post.setMediaUrl(newPresignedUrl);
                postService.update(post);
                log.info("Updated post (ID: {}) with new presigned URL", postId);
            } else {
                log.warn("Post (ID: {}) file key mismatch. Existing: {}, New: {}. Skipping update.", 
                        postId, existingFileKey, fileKey);
            }
        } catch (Exception e) {
            log.error("Error updating post URL for post ID: {}", postId, e);
            throw e; // Re-throw to be caught by fault-tolerant handler
        }
    }

    /**
     * Update gallery image URL in database
     */
    private void updateGalleryImageUrl(Long imageId, String newPresignedUrl, String fileKey) {
        try {
            GalleryImage image = galleryImageService.findById(imageId);
            // Verify the file key matches before updating
            String existingFileKey = extractFileKeyFromUrl(image.getImageUrl());
            if (existingFileKey != null && existingFileKey.equals(fileKey)) {
                image.setImageUrl(newPresignedUrl);
                galleryImageService.update(image);
                log.info("Updated gallery image (ID: {}) with new presigned URL", imageId);
            } else {
                log.warn("Gallery image (ID: {}) file key mismatch. Existing: {}, New: {}. Skipping update.", 
                        imageId, existingFileKey, fileKey);
            }
        } catch (Exception e) {
            log.error("Error updating gallery image URL for image ID: {}", imageId, e);
            throw e; // Re-throw to be caught by fault-tolerant handler
        }
    }

    /**
     * Update album cover image URL in database
     */
    private void updateAlbumUrl(Long albumId, String newPresignedUrl, String fileKey) {
        try {
            Album album = albumService.findById(albumId);
            // Verify the file key matches before updating
            String existingFileKey = extractFileKeyFromUrl(album.getCoverImageUrl());
            if (existingFileKey != null && existingFileKey.equals(fileKey)) {
                album.setCoverImageUrl(newPresignedUrl);
                albumService.update(album);
                log.info("Updated album (ID: {}) with new presigned URL", albumId);
            } else {
                log.warn("Album (ID: {}) file key mismatch. Existing: {}, New: {}. Skipping update.", 
                        albumId, existingFileKey, fileKey);
            }
        } catch (Exception e) {
            log.error("Error updating album URL for album ID: {}", albumId, e);
            throw e; // Re-throw to be caught by fault-tolerant handler
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL or S3 direct URL
     * Supported URL formats:
     * 1. Backblaze B2 file URL: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext?authorization=...
     * 2. S3 direct URL: https://bucket.s3.region.backblazeb2.com/images/timestamp-uuid.ext?X-Amz-...
     * File key format: images/timestamp-uuid.ext
     * 
     * @param url The presigned URL or file key
     * @return Extracted file key or null if not a URL or cannot extract
     */
    private String extractFileKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // If it's not a URL (doesn't start with http:// or https://), assume it's already a file key
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null; // Already a file key, return null so caller uses it as-is
        }

        try {
            String fileKey = null;

            // Handle Backblaze B2 file URLs: https://f001.backblazeb2.com/file/bucket-name/images/...
            if (url.contains("/file/")) {
                int fileIndex = url.indexOf("/file/");
                int bucketStart = fileIndex + 6; // "/file/" length
                int bucketEnd = url.indexOf("/", bucketStart);
                if (bucketEnd != -1) {
                    fileKey = url.substring(bucketEnd + 1);
                }
            }
            // Handle S3 direct URLs: https://bucket.s3.region.backblazeb2.com/images/...
            // Example: https://egramseva.s3.us-east-005.backblazeb2.com/images/1764437021029-38fd8086.jpg
            else if (url.contains(".s3.") && url.contains(".backblazeb2.com")) {
                // Find the domain part and extract path after it
                int s3Index = url.indexOf(".s3.");
                if (s3Index != -1) {
                    // Find the end of domain (first / after .s3.)
                    int domainEnd = url.indexOf("/", s3Index);
                    if (domainEnd != -1) {
                        fileKey = url.substring(domainEnd + 1);
                    } else {
                        // No path, return null
                        return null;
                    }
                }
            }
            // Handle generic S3 URLs: https://bucket.s3.amazonaws.com/images/...
            else if (url.contains(".s3.") || url.contains("s3.amazonaws.com")) {
                try {
                    java.net.URL urlObj = new java.net.URL(url);
                    String path = urlObj.getPath();
                    if (path != null && !path.isEmpty()) {
                        // Remove leading slash
                        fileKey = path.startsWith("/") ? path.substring(1) : path;
                    }
                } catch (java.net.MalformedURLException e) {
                    log.warn("Malformed URL: {}", url, e);
                    return null;
                }
            }

            if (fileKey == null || fileKey.isEmpty()) {
                return null;
            }

            // Remove query parameters if present (from presigned URLs)
            int queryIndex = fileKey.indexOf("?");
            if (queryIndex != -1) {
                fileKey = fileKey.substring(0, queryIndex);
            }

            return fileKey;

        } catch (Exception e) {
            log.warn("Error extracting file key from URL: {}", url, e);
            return null;
        }
    }
}

