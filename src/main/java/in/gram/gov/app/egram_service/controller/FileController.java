package in.gram.gov.app.egram_service.controller;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.service.S3CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    /**
     * Refresh presigned URL for a file
     * This endpoint can be used for any file (gallery images, cover images, attachments, etc.)
     * 
     * @param fileKey The S3 file key/path (e.g., "images/timestamp-uuid.ext")
     *                Can also accept a full presigned URL - will extract the key automatically
     * @return New presigned URL with expiration time
     */
    @GetMapping("/refresh-url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshPresignedUrl(
            @RequestParam String fileKey) {
        
        log.info("Refreshing presigned URL for fileKey: {}", fileKey);
        
        try {
            // Extract file key from URL if a full URL is provided
            String extractedFileKey = extractFileKeyFromUrl(fileKey);
            String actualFileKey = extractedFileKey != null ? extractedFileKey : fileKey;
            
            // Generate new presigned URL
            String newPresignedUrl = s3CloudStorageService.getFileUrl(actualFileKey);
            
            if (newPresignedUrl == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate presigned URL. Cloud storage may not be enabled."));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileKey", actualFileKey);
            response.put("presignedUrl", newPresignedUrl);
            response.put("expiresIn", 3600 * 24); // 24 hours in seconds
            
            log.info("Presigned URL refreshed successfully for fileKey: {}", actualFileKey);
            return ResponseEntity.ok(ApiResponse.success("Presigned URL refreshed successfully", response));
            
        } catch (Exception e) {
            log.error("Error refreshing presigned URL for fileKey: {}", fileKey, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to refresh presigned URL: " + e.getMessage()));
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL
     * URL format: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext?authorization=...
     * File key format: images/timestamp-uuid.ext
     * 
     * @param url The presigned URL or file key
     * @return Extracted file key or null if not a URL
     */
    private String extractFileKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // If it doesn't contain "/file/", assume it's already a file key
        if (!url.contains("/file/")) {
            return null;
        }

        try {
            int fileIndex = url.indexOf("/file/");
            if (fileIndex == -1) {
                return null;
            }

            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = url.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                return null;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = url.substring(bucketEnd + 1);

            if (fileKey.isEmpty()) {
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

