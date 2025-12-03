package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.CloudStorageException;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * S3-compatible service for Backblaze B2 cloud storage using AWS SDK
 * Backblaze B2 provides S3-compatible API endpoints
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3CloudStorageService {

    private final AtomicReference<S3Client> s3ClientRef = new AtomicReference<>();
    private final AtomicReference<S3Presigner> s3PresignerRef = new AtomicReference<>();

    @Value("${backblaze.b2.enabled:false}")
    private boolean b2Enabled;

    @Value("${backblaze.b2.s3-access-key:00559c03c66fe0c0000000002}")
    private String s3AccessKey;

    @Value("${backblaze.b2.s3-secret-key:K005QtotwsqmofAhwgV1pqMdUIMrchw}")
    private String s3SecretKey;

    @Value("${backblaze.b2.s3-endpoint:https://s3.us-west-000.backblazeb2.com}")
    private String s3Endpoint;

    @Value("${backblaze.b2.s3-region:us-west-000}")
    private String s3Region;

    @Value("${backblaze.b2.bucket-name:egram-service}")
    private String bucketName;

    @Value("${backblaze.b2.base-url:https://f001.backblazeb2.com}")
    private String baseUrl;

    @Value("${backblaze.b2.presigned-url-expiration-hours:168}")
    private int presignedUrlExpirationHours;

    /**
     * Initialize S3 client for Backblaze B2
     * @return S3Client instance
     */
    private S3Client getS3Client() {
        if (s3ClientRef.get() == null) {
            synchronized (this) {
                if (s3ClientRef.get() == null) {
                    if (!b2Enabled) {
                        log.warn("Backblaze B2 S3 is not enabled. Check configuration.");
                        throw new CloudStorageException("Backblaze B2 S3 is not enabled");
                    }

                    if (s3AccessKey == null || s3AccessKey.isEmpty() ||
                        s3SecretKey == null || s3SecretKey.isEmpty()) {
                        throw new CloudStorageException("Backblaze B2 S3 credentials are not configured");
                    }

                    try {
                        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(s3AccessKey, s3SecretKey);

                        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                                .region(Region.of(s3Region))
                                .endpointOverride(URI.create(s3Endpoint));

                        S3Client s3Client = s3ClientBuilder.build();
                        s3ClientRef.set(s3Client);
                        log.info("Backblaze B2 S3 client initialized successfully with endpoint: {}", s3Endpoint);
                    } catch (Exception e) {
                        log.error("Failed to initialize Backblaze B2 S3 client", e);
                        throw new CloudStorageException("Failed to initialize S3 client: " + e.getMessage(), e);
                    }
                }
            }
        }
        return s3ClientRef.get();
    }

    /**
     * Initialize S3 Presigner for generating presigned URLs
     * @return S3Presigner instance
     */
    private S3Presigner getS3Presigner() {
        if (s3PresignerRef.get() == null) {
            synchronized (this) {
                if (s3PresignerRef.get() == null) {
                    if (!b2Enabled) {
                        log.warn("Backblaze B2 S3 is not enabled. Check configuration.");
                        throw new CloudStorageException("Backblaze B2 S3 is not enabled");
                    }

                    if (s3AccessKey == null || s3AccessKey.isEmpty() ||
                        s3SecretKey == null || s3SecretKey.isEmpty()) {
                        throw new CloudStorageException("Backblaze B2 S3 credentials are not configured");
                    }

                    try {
                        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(s3AccessKey, s3SecretKey);

                        S3Presigner presigner = S3Presigner.builder()
                                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                                .region(Region.of(s3Region))
                                .endpointOverride(URI.create(s3Endpoint))
                                .build();

                        s3PresignerRef.set(presigner);
                        log.info("Backblaze B2 S3 Presigner initialized successfully");
                    } catch (Exception e) {
                        log.error("Failed to initialize Backblaze B2 S3 Presigner", e);
                        throw new CloudStorageException("Failed to initialize S3 Presigner: " + e.getMessage(), e);
                    }
                }
            }
        }
        return s3PresignerRef.get();
    }

    /**
     * Upload compressed image to Backblaze B2 via S3-compatible API
     * Generates presigned URL for private bucket access
     * @param compressedImageStream InputStream of compressed image
     * @param compressionMetadata ImageCompressionDTO with metadata
     * @return Updated ImageCompressionDTO with presigned URL
     */
    public ImageCompressionDTO uploadImageToB2(
            InputStream compressedImageStream,
            ImageCompressionDTO compressionMetadata) {
        try {
            if (!b2Enabled) {
                log.warn("Backblaze B2 S3 upload skipped - service not enabled");
                return compressionMetadata;
            }

            S3Client s3Client = getS3Client();

            // Generate unique file name to avoid conflicts
            String originalFileName = compressionMetadata.getOriginalFileName();
            String fileExtension = FilenameUtils.getExtension(originalFileName);
            String uniqueFileName = generateUniqueFileName(fileExtension);

            log.info("Starting upload to Backblaze B2 via S3. File: {}, Bucket: {}", uniqueFileName, bucketName);

            // Read image bytes from stream
            byte[] imageBytes = compressedImageStream.readAllBytes();

            // Prepare S3 PUT request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(compressionMetadata.getContentType())
                    .metadata(java.util.Map.of(
                            "original-filename", originalFileName,
                            "upload-timestamp", String.valueOf(System.currentTimeMillis()),
                            "compression-ratio", String.format("%.2f", compressionMetadata.getCompressionRatio())
                    ))
                    .build();

            // Upload file
            PutObjectResponse response = s3Client.putObject(putObjectRequest,
                    RequestBody.fromBytes(imageBytes));

            log.info("File uploaded successfully to Backblaze B2 via S3. ETag: {}", response.eTag());

            // Generate presigned URL for private bucket access
            String presignedUrl = generatePresignedUrl(uniqueFileName);

            log.info("Presigned URL generated for file: {}", uniqueFileName);

            // Update compression metadata with B2 information
            compressionMetadata.setBackblazeFileId(uniqueFileName);
            compressionMetadata.setBackblazeFileUrl(presignedUrl);

            return compressionMetadata;

        } catch (IOException e) {
            log.error("IO error during Backblaze B2 S3 upload", e);
            throw new CloudStorageException("IO error during S3 upload: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error uploading image to Backblaze B2 via S3", e);
            throw new CloudStorageException("Failed to upload image to B2: " + e.getMessage(), e);
        }
    }

    /**
     * Delete file from Backblaze B2 via S3-compatible API
     * @param fileKey S3 key/path of file to delete
     */
    public void deleteImageFromB2(String fileKey) {
        try {
            if (!b2Enabled) {
                log.warn("Backblaze B2 S3 delete skipped - service not enabled");
                return;
            }

            S3Client s3Client = getS3Client();

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from Backblaze B2 via S3. Key: {}", fileKey);

        } catch (Exception e) {
            log.error("Error deleting file from Backblaze B2 via S3", e);
            throw new CloudStorageException("Failed to delete file from B2: " + e.getMessage(), e);
        }
    }

    /**
     * Get presigned URL for uploaded image (valid for configured hours)
     * For private buckets, presigned URLs are required for access
     * @param fileKey S3 key/path of file
     * @return Presigned URL with expiration time, or null if generation fails
     */
    public String getFileUrl(String fileKey) {
        if (!b2Enabled) {
            log.warn("B2 is not enabled, cannot generate presigned URL for fileKey: {}", fileKey);
            return null;
        }
        
        if (fileKey == null || fileKey.trim().isEmpty()) {
            log.error("Cannot generate presigned URL: fileKey is null or empty");
            return null;
        }
        
        try {
            String presignedUrl = generatePresignedUrl(fileKey);
            if (presignedUrl == null || presignedUrl.isEmpty()) {
                log.error("Generated presigned URL is null or empty for fileKey: {}", fileKey);
                return null;
            }
            log.debug("Successfully generated presigned URL for fileKey: {}", fileKey);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Exception generating presigned URL for fileKey: {}", fileKey, e);
            return null;
        }
    }

    /**
     * Generate presigned URL for S3 object access
     * URL will be valid for the configured duration (default: 7 days / 168 hours)
     * @param fileKey S3 key/path of file
     * @return Presigned URL string
     */
    private String generatePresignedUrl(String fileKey) {
        try {
            S3Presigner presigner = getS3Presigner();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(presignedUrlExpirationHours))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.debug("Presigned URL generated for key: {} with {} hours expiration",
                    fileKey, presignedUrlExpirationHours);

            return presignedUrl;

        } catch (Exception e) {
            log.error("Error generating presigned URL for file key: {}", fileKey, e);
            throw new CloudStorageException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    /**
     * Check if B2 is enabled and configured
     * @return true if enabled and configured, false otherwise
     */
    public boolean isB2Enabled() {
        return b2Enabled && s3AccessKey != null && !s3AccessKey.isEmpty()
                && s3SecretKey != null && !s3SecretKey.isEmpty();
    }

    /**
     * Get the configured presigned URL expiration time in seconds
     * @return Expiration time in seconds
     */
    public int getPresignedUrlExpirationSeconds() {
        return presignedUrlExpirationHours * 3600; // Convert hours to seconds
    }

    /**
     * Generate unique file name to avoid conflicts
     * @param fileExtension File extension
     * @return Unique file name with timestamp and UUID
     */
    private String generateUniqueFileName(String fileExtension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("images/%s-%s.%s", timestamp, uuid, fileExtension);
    }

    /**
     * Regenerate presigned URL for existing file when current URL expires
     * Use this when the presigned URL has expired or needs to be refreshed
     * @param fileKey S3 key/path of file
     * @return ImageCompressionDTO with newly generated presigned URL
     */
    public ImageCompressionDTO regeneratePresignedUrl(String fileKey) {
        try {
            if (!b2Enabled) {
                log.warn("B2 is not enabled, cannot regenerate presigned URL");
                return null;
            }

            log.info("Regenerating presigned URL for file key: {}", fileKey);
            String newPresignedUrl = generatePresignedUrl(fileKey);

            ImageCompressionDTO result = new ImageCompressionDTO();
            result.setBackblazeFileId(fileKey);
            result.setBackblazeFileUrl(newPresignedUrl);

            log.info("Presigned URL regenerated successfully for file key: {}", fileKey);
            return result;

        } catch (Exception e) {
            log.error("Error regenerating presigned URL for file key: {}", fileKey, e);
            throw new CloudStorageException("Failed to regenerate presigned URL: " + e.getMessage(), e);
        }
    }

    /**
     * Close S3 client and presigner connections
     */
    public void closeClient() {
        S3Client s3Client = s3ClientRef.get();
        if (s3Client != null) {
            s3Client.close();
            s3ClientRef.set(null);
            log.info("Backblaze B2 S3 client closed");
        }

        S3Presigner presigner = s3PresignerRef.get();
        if (presigner != null) {
            presigner.close();
            s3PresignerRef.set(null);
            log.info("Backblaze B2 S3 Presigner closed");
        }
    }
}

