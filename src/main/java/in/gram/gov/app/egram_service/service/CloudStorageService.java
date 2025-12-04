package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.CloudStorageException;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Main Cloud Storage Service - acts as a facade to delegate to S3CloudStorageService
 * Uses Cloudflare R2 S3-compatible API with public URL access
 * This service provides a unified interface for cloud storage operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudStorageService {

    private final S3CloudStorageService s3CloudStorageService;

    @Value("${cloudflare.r2.enabled:false}")
    private boolean r2Enabled;

    /**
     * Upload compressed image to Cloudflare R2 cloud storage via S3-compatible API
     * @param compressedImageStream InputStream of compressed image
     * @param compressionMetadata ImageCompressionDTO with metadata
     * @return Updated ImageCompressionDTO with cloud file information and public URL
     * @throws CloudStorageException if upload fails
     */
    public ImageCompressionDTO uploadImage(
            InputStream compressedImageStream,
            ImageCompressionDTO compressionMetadata) {
        try {
            log.info("Starting image upload to Cloudflare R2 cloud storage: {}", compressionMetadata.getOriginalFileName());

            if (!r2Enabled) {
                log.warn("Cloud storage is not enabled. Image will not be persisted to Cloudflare R2");
                return compressionMetadata;
            }

            // Delegate to S3 service using Cloudflare R2 S3-compatible API
            ImageCompressionDTO result = s3CloudStorageService.uploadImageToB2(
                    compressedImageStream,
                    compressionMetadata
            );

            log.info("Image uploaded successfully to Cloudflare R2. Public URL: {}", result.getBackblazeFileUrl());
            return result;

        } catch (Exception e) {
            log.error("Failed to upload image to Cloudflare R2 cloud storage", e);
            throw new CloudStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete image from cloud storage
     * @param fileKey S3 key/path of the file to delete (e.g., "images/1234567890-abcd1234.jpg")
     * @throws CloudStorageException if deletion fails
     */
    public void deleteImage(String fileKey) {
        try {
            if (!r2Enabled) {
                log.warn("Cloud storage is not enabled. Skipping delete operation");
                return;
            }

            log.info("Deleting image from Cloudflare R2 cloud storage. File Key: {}", fileKey);
            s3CloudStorageService.deleteImageFromB2(fileKey);
            log.info("Image deleted successfully from Cloudflare R2");

        } catch (Exception e) {
            log.error("Failed to delete image from Cloudflare R2 cloud storage", e);
            throw new CloudStorageException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    /**
     * Get public URL for uploaded image
     * Returns permanent public URL for accessing the file
     * @param fileKey File key/path in R2 (e.g., "images/1234567890-abcd1234.jpg")
     * @return Public URL for accessing the image (e.g., "https://pub-xxxxx.r2.dev/images/1234567890-abcd1234.jpg")
     */
    public String getImageUrl(String fileKey) {
        if (!r2Enabled) {
            log.warn("R2 is not enabled, cannot generate image URL");
            return null;
        }

        if (fileKey == null || fileKey.trim().isEmpty()) {
            log.warn("File key is null or empty, cannot generate image URL");
            return null;
        }

        return s3CloudStorageService.getFileUrl(fileKey);
    }

    /**
     * Check if cloud storage is enabled and configured
     * @return true if enabled and properly configured with public domain
     */
    public boolean isEnabled() {
        return r2Enabled && s3CloudStorageService.isB2Enabled();
    }

    /**
     * Regenerate public URL for existing file
     * Useful for getting the current public URL of a file
     * @param fileKey File key/path in R2
     * @return ImageCompressionDTO with public URL
     */
    public ImageCompressionDTO regeneratePresignedUrl(String fileKey) {
        if (!r2Enabled) {
            log.warn("R2 is not enabled, cannot regenerate URL");
            return null;
        }

        log.info("Regenerating public URL for file key: {}", fileKey);
        return s3CloudStorageService.regeneratePresignedUrl(fileKey);
    }

    /**
     * Extract file key from full public URL
     * Helper method to extract the file key from a complete R2 public URL
     * Example: "https://pub-xxxxx.r2.dev/images/1234567890-abcd1234.jpg" -> "images/1234567890-abcd1234.jpg"
     * @param publicUrl Full public URL of the file
     * @return File key (path after domain), or null if invalid URL
     */
    public String extractFileKeyFromUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.trim().isEmpty()) {
            log.warn("Public URL is null or empty");
            return null;
        }

        try {
            // Extract everything after the domain
            // Example: https://pub-xxxxx.r2.dev/images/file.jpg -> images/file.jpg
            int lastSlashIndex = publicUrl.indexOf('/', 8); // Skip "https://"
            if (lastSlashIndex > 0 && lastSlashIndex < publicUrl.length() - 1) {
                String fileKey = publicUrl.substring(lastSlashIndex + 1);
                log.debug("Extracted file key '{}' from URL '{}'", fileKey, publicUrl);
                return fileKey;
            } else {
                log.warn("Could not extract file key from URL: {}", publicUrl);
                return null;
            }
        } catch (Exception e) {
            log.error("Error extracting file key from URL: {}", publicUrl, e);
            return null;
        }
    }
}