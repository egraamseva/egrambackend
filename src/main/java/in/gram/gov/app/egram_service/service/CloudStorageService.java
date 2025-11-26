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
 * Uses Backblaze B2 S3-compatible API with Backblaze credentials
 * This service provides a unified interface for cloud storage operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudStorageService {

    private final S3CloudStorageService s3CloudStorageService;

    @Value("${backblaze.b2.enabled:false}")
    private boolean b2Enabled;

    /**
     * Upload compressed image to Backblaze B2 cloud storage via S3-compatible API
     * @param compressedImageStream InputStream of compressed image
     * @param compressionMetadata ImageCompressionDTO with metadata
     * @return Updated ImageCompressionDTO with cloud file information
     * @throws CloudStorageException if upload fails
     */
    public ImageCompressionDTO uploadImage(
            InputStream compressedImageStream,
            ImageCompressionDTO compressionMetadata) {
        try {
            log.info("Starting image upload to Backblaze B2 cloud storage: {}", compressionMetadata.getOriginalFileName());

            if (!b2Enabled) {
                log.warn("Cloud storage is not enabled. Image will not be persisted to Backblaze B2");
                return compressionMetadata;
            }

            // Delegate to S3 service using Backblaze B2 S3-compatible API
            ImageCompressionDTO result = s3CloudStorageService.uploadImageToB2(
                    compressedImageStream,
                    compressionMetadata
            );

            log.info("Image uploaded successfully to Backblaze B2. URL: {}", result.getBackblazeFileUrl());
            return result;

        } catch (Exception e) {
            log.error("Failed to upload image to Backblaze B2 cloud storage", e);
            throw new CloudStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete image from cloud storage
     * @param fileKey S3 key/path of the file to delete (typically extracted from URL)
     * @throws CloudStorageException if deletion fails
     */
    public void deleteImage(String fileKey) {
        try {
            if (!b2Enabled) {
                log.warn("Cloud storage is not enabled. Skipping delete operation");
                return;
            }

            log.info("Deleting image from Backblaze B2 cloud storage. File Key: {}", fileKey);
            s3CloudStorageService.deleteImageFromB2(fileKey);
            log.info("Image deleted successfully from Backblaze B2");

        } catch (Exception e) {
            log.error("Failed to delete image from Backblaze B2 cloud storage", e);
            throw new CloudStorageException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    /**
     * Get public URL for uploaded image
     * @param fileName Backblaze B2 file name
     * @return Public URL for accessing the image
     */
    public String getImageUrl(String fileName) {
        if (!b2Enabled) {
            return null;
        }
        return s3CloudStorageService.getFileUrl(fileName);
    }

    /**
     * Check if cloud storage is enabled and configured
     * @return true if enabled and properly configured
     */
    public boolean isEnabled() {
        return b2Enabled && s3CloudStorageService.isB2Enabled();
    }

    public ImageCompressionDTO regeneratePresignedUrl(String fileKey) {
        return s3CloudStorageService.regeneratePresignedUrl(fileKey);
    }
}

