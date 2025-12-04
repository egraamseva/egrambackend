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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * S3-compatible service for Cloudflare R2 cloud storage using AWS SDK
 * Cloudflare R2 provides S3-compatible API endpoints with public URL access
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3CloudStorageService {

    private final AtomicReference<S3Client> s3ClientRef = new AtomicReference<>();

    @Value("${cloudflare.r2.enabled:false}")
    private boolean r2Enabled;

    @Value("${cloudflare.r2.access-key-id}")
    private String accessKeyId;

    @Value("${cloudflare.r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.account-id}")
    private String accountId;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.public-domain}")
    private String publicDomain;

    /**
     * Initialize S3 client for Cloudflare R2
     * @return S3Client instance
     */
    private S3Client getS3Client() {
        if (s3ClientRef.get() == null) {
            synchronized (this) {
                if (s3ClientRef.get() == null) {
                    if (!r2Enabled) {
                        log.warn("Cloudflare R2 is not enabled. Check configuration.");
                        throw new CloudStorageException("Cloudflare R2 is not enabled");
                    }

                    if (accessKeyId == null || accessKeyId.isEmpty() ||
                            secretAccessKey == null || secretAccessKey.isEmpty()) {
                        throw new CloudStorageException("Cloudflare R2 credentials are not configured");
                    }

                    if (publicDomain == null || publicDomain.isEmpty()) {
                        throw new CloudStorageException("Cloudflare R2 public domain is not configured");
                    }

                    try {
                        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

                        // Cloudflare R2 endpoint format: https://<account-id>.r2.cloudflarestorage.com
                        String r2Endpoint = endpoint != null && !endpoint.isEmpty()
                                ? endpoint
                                : String.format("https://%s.r2.cloudflarestorage.com", accountId);

                        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                                .region(Region.of("auto")) // Cloudflare R2 uses 'auto' region
                                .endpointOverride(URI.create(r2Endpoint));

                        S3Client s3Client = s3ClientBuilder.build();
                        s3ClientRef.set(s3Client);
                        log.info("Cloudflare R2 S3 client initialized successfully with endpoint: {}", r2Endpoint);
                        log.info("Public domain configured: {}", publicDomain);
                    } catch (Exception e) {
                        log.error("Failed to initialize Cloudflare R2 S3 client", e);
                        throw new CloudStorageException("Failed to initialize S3 client: " + e.getMessage(), e);
                    }
                }
            }
        }
        return s3ClientRef.get();
    }

    /**
     * Upload compressed image to Cloudflare R2 via S3-compatible API
     * Returns public URL for the uploaded file
     * @param compressedImageStream InputStream of compressed image
     * @param compressionMetadata ImageCompressionDTO with metadata
     * @return Updated ImageCompressionDTO with public URL
     */
    public ImageCompressionDTO uploadImageToB2(
            InputStream compressedImageStream,
            ImageCompressionDTO compressionMetadata) {
        try {
            if (!r2Enabled) {
                log.warn("Cloudflare R2 upload skipped - service not enabled");
                return compressionMetadata;
            }

            S3Client s3Client = getS3Client();

            // Generate unique file name to avoid conflicts
            String originalFileName = compressionMetadata.getOriginalFileName();
            String fileExtension = FilenameUtils.getExtension(originalFileName);
            String uniqueFileName = generateUniqueFileName(fileExtension);

            log.info("Starting upload to Cloudflare R2 via S3. File: {}, Bucket: {}", uniqueFileName, bucketName);

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

            log.info("File uploaded successfully to Cloudflare R2 via S3. ETag: {}", response.eTag());

            // Generate public URL
            String publicUrl = getFileUrl(uniqueFileName);

            log.info("Public URL generated for file: {}", uniqueFileName);

            // Update compression metadata with R2 information
            compressionMetadata.setBackblazeFileId(uniqueFileName);
            compressionMetadata.setBackblazeFileUrl(publicUrl);

            return compressionMetadata;

        } catch (IOException e) {
            log.error("IO error during Cloudflare R2 S3 upload", e);
            throw new CloudStorageException("IO error during S3 upload: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error uploading image to Cloudflare R2 via S3", e);
            throw new CloudStorageException("Failed to upload image to R2: " + e.getMessage(), e);
        }
    }

    /**
     * Delete file from Cloudflare R2 via S3-compatible API
     * @param fileKey S3 key/path of file to delete
     */
    public void deleteImageFromB2(String fileKey) {
        try {
            if (!r2Enabled) {
                log.warn("Cloudflare R2 delete skipped - service not enabled");
                return;
            }

            S3Client s3Client = getS3Client();

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from Cloudflare R2 via S3. Key: {}", fileKey);

        } catch (Exception e) {
            log.error("Error deleting file from Cloudflare R2 via S3", e);
            throw new CloudStorageException("Failed to delete file from R2: " + e.getMessage(), e);
        }
    }

    /**
     * Get public URL for uploaded image
     * @param fileKey S3 key/path of file
     * @return Public URL, or null if configuration is invalid
     */
    public String getFileUrl(String fileKey) {
        if (!r2Enabled) {
            log.warn("R2 is not enabled, cannot generate URL for fileKey: {}", fileKey);
            return null;
        }

        if (fileKey == null || fileKey.trim().isEmpty()) {
            log.error("Cannot generate URL: fileKey is null or empty");
            return null;
        }

        if (publicDomain == null || publicDomain.isEmpty()) {
            log.error("Cannot generate URL: public domain is not configured");
            return null;
        }

        try {
            // Generate public URL using configured domain
            String publicUrl = String.format("%s/%s", publicDomain.replaceAll("/$", ""), fileKey);
            log.debug("Generated public URL for fileKey: {}", fileKey);
            return publicUrl;
        } catch (Exception e) {
            log.error("Exception generating URL for fileKey: {}", fileKey, e);
            return null;
        }
    }

    /**
     * Check if R2 is enabled and configured
     * @return true if enabled and configured, false otherwise
     */
    public boolean isB2Enabled() {
        return r2Enabled
                && accessKeyId != null && !accessKeyId.isEmpty()
                && secretAccessKey != null && !secretAccessKey.isEmpty()
                && publicDomain != null && !publicDomain.isEmpty();
    }

    /**
     * Get the configured presigned URL expiration time in seconds
     * @return Always returns 0 as presigned URLs are not used (public URLs instead)
     * @deprecated Public URLs are used instead of presigned URLs
     */
    @Deprecated
    public int getPresignedUrlExpirationSeconds() {
        return 0; // Not applicable for public URLs
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
     * Regenerate URL for existing file
     * @param fileKey S3 key/path of file
     * @return ImageCompressionDTO with public URL
     */
    public ImageCompressionDTO regeneratePresignedUrl(String fileKey) {
        try {
            if (!r2Enabled) {
                log.warn("R2 is not enabled, cannot regenerate URL");
                return null;
            }

            log.info("Regenerating public URL for file key: {}", fileKey);
            String publicUrl = getFileUrl(fileKey);

            if (publicUrl == null) {
                log.error("Failed to generate public URL for file key: {}", fileKey);
                return null;
            }

            ImageCompressionDTO result = new ImageCompressionDTO();
            result.setBackblazeFileId(fileKey);
            result.setBackblazeFileUrl(publicUrl);

            log.info("Public URL regenerated successfully for file key: {}", fileKey);
            return result;

        } catch (Exception e) {
            log.error("Error regenerating URL for file key: {}", fileKey, e);
            throw new CloudStorageException("Failed to regenerate URL: " + e.getMessage(), e);
        }
    }

    /**
     * Close S3 client connections
     */
    public void closeClient() {
        S3Client s3Client = s3ClientRef.get();
        if (s3Client != null) {
            s3Client.close();
            s3ClientRef.set(null);
            log.info("Cloudflare R2 S3 client closed");
        }
    }
}