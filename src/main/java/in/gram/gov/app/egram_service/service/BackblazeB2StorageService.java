//package in.gram.gov.app.egram_service.service;
//
//
//import com.backblaze.b2.client.webApiClients.B2WebApiClient;
//import in.gram.gov.app.egram_service.constants.exception.CloudStorageException;
//import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FilenameUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicReference;
//
///**
// * Backblaze B2 Cloud Storage Service using native B2 API
// * Uses only Backblaze B2 credentials - no AWS S3 needed
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class BackblazeB2StorageService {
//
//    private final AtomicReference<B2WebApiClient> b2ApiRef = new AtomicReference<>();
//
//    @Value("${backblaze.b2.enabled:false}")
//    private boolean b2Enabled;
//
//    @Value("${backblaze.b2.app-key-id:}")
//    private String appKeyId;
//
//    @Value("${backblaze.b2.app-key:}")
//    private String appKey;
//
//    @Value("${backblaze.b2.bucket-name:egram-service}")
//    private String bucketName;
//
//    @Value("${backblaze.b2.bucket-id:}")
//    private String bucketId;
//
//    @Value("${backblaze.b2.base-url:https://f001.backblazeb2.com}")
//    private String baseUrl;
//
//    /**
//     * Initialize B2 API connection using native B2 SDK
//     * Only requires Backblaze B2 credentials
//     * @return B2Api instance
//     */
//    private B2WebApiClient getB2Api() {
//        if (b2ApiRef.get() == null) {
//            synchronized (this) {
//                if (b2ApiRef.get() == null) {
//                    if (!b2Enabled) {
//                        log.warn("Backblaze B2 is not enabled. Check configuration.");
//                        throw new CloudStorageException("Backblaze B2 is not enabled");
//                    }
//
//                    if (appKeyId == null || appKeyId.isEmpty() || appKey == null || appKey.isEmpty()) {
//                        throw new CloudStorageException("Backblaze B2 credentials (app-key-id and app-key) are not configured");
//                    }
//
//                    try {
//                        B2Api b2Api = new B2ApiFactory().create(appKeyId, appKey, "EgramService/1.0");
//                        b2ApiRef.set(b2Api);
//                        log.info("Backblaze B2 API initialized successfully using native B2 SDK");
//                    } catch (B2ApiException | B2ApiRuntimeException e) {
//                        log.error("Failed to initialize Backblaze B2 API", e);
//                        throw new CloudStorageException("Failed to initialize B2 API: " + e.getMessage(), e);
//                    }
//                }
//            }
//        }
//        return b2ApiRef.get();
//    }
//
//    /**
//     * Upload compressed image to Backblaze B2 using native B2 API
//     * @param compressedImageStream InputStream of compressed image
//     * @param compressionMetadata ImageCompressionDTO with metadata
//     * @return Updated ImageCompressionDTO with B2 file information
//     */
//    public ImageCompressionDTO uploadImageToB2(
//            InputStream compressedImageStream,
//            ImageCompressionDTO compressionMetadata) {
//        try {
//            if (!b2Enabled) {
//                log.warn("Backblaze B2 upload skipped - service not enabled");
//                return compressionMetadata;
//            }
//
//            B2Api b2Api = getB2Api();
//
//            // Generate unique file name to avoid conflicts
//            String originalFileName = compressionMetadata.getOriginalFileName();
//            String fileExtension = FilenameUtils.getExtension(originalFileName);
//            String uniqueFileName = generateUniqueFileName(fileExtension);
//
//            log.info("Starting upload to Backblaze B2 using native API. File: {}, Bucket: {}", uniqueFileName, bucketName);
//
//            // Prepare file metadata
//            Map<String, String> fileInfo = new HashMap<>();
//            fileInfo.put("original-filename", originalFileName);
//            fileInfo.put("upload-timestamp", String.valueOf(System.currentTimeMillis()));
//            fileInfo.put("compression-ratio", String.format("%.2f", compressionMetadata.getCompressionRatio()));
//
//            // Create content source from input stream
//            long fileSize = compressionMetadata.getCompressedFileSize();
//            B2ContentSource contentSource = B2ContentSourceFactory.createContentSource(
//                    compressedImageStream,
//                    fileSize,
//                    compressionMetadata.getContentType()
//            );
//
//            // Upload file to B2
//            B2FileVersion fileVersion = b2Api.getUploadUrl(bucketId)
//                    .uploadFile(
//                            contentSource,
//                            uniqueFileName,
//                            compressionMetadata.getContentType(),
//                            fileInfo
//                    );
//
//            // Generate public file URL
//            String fileUrl = String.format("%s/file/%s/%s", baseUrl, bucketName, uniqueFileName);
//
//            log.info("File uploaded successfully to Backblaze B2. File ID: {}, URL: {}",
//                    fileVersion.getFileId(), fileUrl);
//
//            // Update compression metadata with B2 information
//            compressionMetadata.setBackblazeFileId(fileVersion.getFileId());
//            compressionMetadata.setBackblazeFileUrl(fileUrl);
//
//            return compressionMetadata;
//
//        } catch (B2ApiException | B2ApiRuntimeException e) {
//            log.error("B2 API error during image upload", e);
//            throw new CloudStorageException("Failed to upload image to B2: " + e.getMessage(), e);
//        } catch (IOException e) {
//            log.error("IO error during B2 upload", e);
//            throw new CloudStorageException("IO error during B2 upload: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Delete file from Backblaze B2 using native B2 API
//     * @param fileId B2 file ID to delete
//     * @param fileName B2 file name to delete
//     */
//    public void deleteImageFromB2(String fileId, String fileName) {
//        try {
//            if (!b2Enabled) {
//                log.warn("Backblaze B2 delete skipped - service not enabled");
//                return;
//            }
//
//            B2Api b2Api = getB2Api();
//            b2Api.getFileVersion(fileId).delete();
//            log.info("File deleted successfully from Backblaze B2. File ID: {}, File Name: {}", fileId, fileName);
//
//        } catch (B2ApiException | B2ApiRuntimeException e) {
//            log.error("Error deleting file from Backblaze B2", e);
//            throw new CloudStorageException("Failed to delete file from B2: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Get public file URL
//     * @param fileName Name of file in B2
//     * @return Public download URL
//     */
//    public String getFileUrl(String fileName) {
//        if (!b2Enabled) {
//            return null;
//        }
//        return String.format("%s/file/%s/%s", baseUrl, bucketName, fileName);
//    }
//
//    /**
//     * Check if B2 is enabled and configured
//     * @return true if enabled and configured with required credentials
//     */
//    public boolean isB2Enabled() {
//        return b2Enabled && appKeyId != null && !appKeyId.isEmpty()
//                && appKey != null && !appKey.isEmpty() && bucketId != null && !bucketId.isEmpty();
//    }
//
//    /**
//     * Generate unique file name to avoid conflicts
//     * @param fileExtension File extension
//     * @return Unique file name with timestamp and UUID
//     */
//    private String generateUniqueFileName(String fileExtension) {
//        String timestamp = String.valueOf(System.currentTimeMillis());
//        String uuid = UUID.randomUUID().toString().substring(0, 8);
//        return String.format("images/%s-%s.%s", timestamp, uuid, fileExtension);
//    }
//
//    /**
//     * Close B2 API connection
//     */
//    public void closeConnection() {
//        B2Api b2Api = b2ApiRef.get();
//        if (b2Api != null) {
//            try {
//                b2Api.close();
//                b2ApiRef.set(null);
//                log.info("Backblaze B2 API connection closed");
//            } catch (Exception e) {
//                log.warn("Error closing B2 API connection", e);
//            }
//        }
//    }
//}
//
