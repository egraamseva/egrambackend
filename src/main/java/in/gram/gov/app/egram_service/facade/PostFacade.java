package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.enums.PostStatus;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.Post;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.dto.filters.PostFilter;
import in.gram.gov.app.egram_service.dto.request.PostRequestDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.dto.response.PostResponseDTO;
import in.gram.gov.app.egram_service.service.*;
import in.gram.gov.app.egram_service.transformer.PostTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j

public class PostFacade {
    private final PostService postService;
    private final PanchayatService panchayatService;
    private final UserService userService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;

    /**
     * Create a new post with optional image upload
     * If image is provided: compress it, upload to B2, and store URL in DB
     */
    @Transactional
    public PostResponseDTO create(PostRequestDTO request, String email) {
        log.info("PostFacade.create called - title={}, email={}", request.getTitle(), email);
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);
        User author = userService.findByEmail(email);

        // Handle image upload if provided
        String mediaUrl = request.getMediaUrl();
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            mediaUrl = processAndUploadImage(request.getImageFile(), request.getCompressionQuality());
        }

        Post post = Post.builder()
                .title(request.getTitle())
                .bodyText(request.getBodyText())
                .mediaUrl(mediaUrl)
                .panchayat(panchayat)
                .author(author)
                .status(PostStatus.PUBLISHED)
                .viewCount(0L)
                .build();

        post = postService.create(post);
        log.info("Post created successfully with ID: {}", post.getId());
        return mapToResponse(post);
    }

    public PostResponseDTO getById(Long id) {
        log.debug("PostFacade.getById called - id={}", id);
        Post post = postService.findById(id);
        return mapToResponse(post);
    }

    public Page<PostResponseDTO> getAll(PostFilter postFilter) {
        log.info("PostFacade.getAll called - filter={}", postFilter);
        Long tenantId = TenantContext.getTenantId();
        Page<Post> posts = postService.findAllByFilter(postFilter);
        return posts.map(this::mapToResponse);
    }

    public Page<PostResponseDTO> getPublishedBySlug(String slug, Pageable pageable) {
        log.info("PostFacade.getPublishedBySlug called - slug={}, pageable={}", slug, pageable);
        Page<Post> posts = postService.findPublishedBySlug(slug, pageable);
        return posts.map(this::mapToResponse);
    }

    public PostResponseDTO getPublishedByIdAndSlug(Long postId, String slug) {
        log.info("PostFacade.getPublishedByIdAndSlug called - postId={}, slug={}", postId, slug);
        Post post = postService.findPublishedByIdAndSlug(postId, slug);
        postService.incrementViewCount(postId);
        return mapToResponse(post);
    }

    /**
     * Update a post with optional new image
     * If new image provided: compress, upload to B2, delete old image, and update URL
     */
    @Transactional
    public PostResponseDTO update(Long id, PostRequestDTO request) {
        log.info("PostFacade.update called - id={}", id);
        Post post = postService.findById(id);

        post.setTitle(request.getTitle());
        post.setBodyText(request.getBodyText());

        // Handle image update if new image provided
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            // Delete old image if exists
            if (post.getMediaUrl() != null && cloudStorageService.isEnabled()) {
                deleteImageFile(post.getMediaUrl());
            }
            // Upload new image
            String newMediaUrl = processAndUploadImage(request.getImageFile(), request.getCompressionQuality());
            post.setMediaUrl(newMediaUrl);
        } else if (request.getMediaUrl() != null) {
            // Update with provided URL if no new file
            post.setMediaUrl(request.getMediaUrl());
        }

        post = postService.update(post);
        log.info("Post updated successfully with ID: {}", id);
        return mapToResponse(post);
    }

    @Transactional
    public void publish(Long id) {
        log.info("PostFacade.publish called - id={}", id);
        postService.publish(id);
    }

    /**
     * Refresh/regenerate presigned URL for post image when expired
     * Extracts the file key from current URL and generates a new presigned URL
     * @param id Post ID
     * @return Updated post with fresh presigned URL
     */
    @Transactional
    public PostResponseDTO refreshImageUrl(Long id) {
        log.info("PostFacade.refreshImageUrl called - id={}", id);
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
                throw new IllegalArgumentException("Cannot extract file key from URL: " + post.getMediaUrl());
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

            return mapToResponse(post);

        } catch (Exception e) {
            log.error("Error refreshing image URL for post ID: {}", id, e);
            throw new RuntimeException("Failed to refresh image URL: " + e.getMessage(), e);
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL
     * URL format: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext
     * File key format: images/timestamp-uuid.ext
     * @param mediaUrl The presigned URL
     * @return Extracted file key or null if invalid URL format
     */
    private String extractFileKeyFromUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            return null;
        }

        try {
            int fileIndex = mediaUrl.indexOf("/file/");
            if (fileIndex == -1) {
                log.warn("Invalid media URL format - '/file/' not found: {}", mediaUrl);
                return null;
            }

            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = mediaUrl.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                log.warn("Cannot extract bucket name from URL: {}", mediaUrl);
                return null;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = mediaUrl.substring(bucketEnd + 1);

            if (fileKey.isEmpty()) {
                log.warn("Extracted file key is empty from URL: {}", mediaUrl);
                return null;
            }

            // Remove query parameters if present (from presigned URLs)
            int queryIndex = fileKey.indexOf("?");
            if (queryIndex != -1) {
                fileKey = fileKey.substring(0, queryIndex);
            }

            return fileKey;

        } catch (Exception e) {
            log.error("Error extracting file key from URL: {}", mediaUrl, e);
            return null;
        }
    }

    /**
     * Delete post and associated image from cloud storage
     */
    @Transactional
    public void delete(Long id) {
        log.info("PostFacade.delete called - id={}", id);
        Post post = postService.findById(id);

        // Delete image from cloud storage if exists
        if (post.getMediaUrl() != null && cloudStorageService.isEnabled()) {
            deleteImageFile(post.getMediaUrl());
        }

        postService.delete(id);
        log.info("Post deleted successfully with ID: {}", id);
    }

    /**
     * Process image: compress and upload to Backblaze B2
     * @param imageFile MultipartFile to process
     * @param compressionQuality Quality level for compression
     * @return URL of uploaded image or null if storage is disabled
     */
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
                    imageFile,
                    compressionQuality
            );

            log.info("Image compressed successfully. Original: {} bytes, Compressed: {} bytes",
                    compressionMetadata.getOriginalFileSize(),
                    compressionMetadata.getCompressedFileSize());

            // Step 3: Get compressed image stream
            InputStream compressedImageStream = imageCompressionService.getCompressedImageInputStream(
                    imageFile,
                    compressionQuality
            );

            // Step 4: Upload to Backblaze B2
            ImageCompressionDTO uploadResult = cloudStorageService.uploadImage(
                    compressedImageStream,
                    compressionMetadata
            );

            log.info("Image uploaded to cloud storage. URL: {}", uploadResult.getBackblazeFileUrl());
            return uploadResult.getBackblazeFileUrl();

        } catch (Exception e) {
            log.error("Error processing image for upload", e);
            // Don't fail the entire post creation if image upload fails
            // Return null and the post will be created without image
            return null;
        }
    }

    /**
     * Delete image from cloud storage
     * Extracts S3 key (file path) from the stored URL
     * File URL format: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext
     * File key format: images/timestamp-uuid.ext
     */
    private void deleteImageFile(String mediaUrl) {
        try {
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                return;
            }

            // Extract file key from URL
            // Format: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext
            // We need: images/timestamp-uuid.ext
            int fileIndex = mediaUrl.indexOf("/file/");
            if (fileIndex == -1) {
                log.warn("Invalid media URL format: {}", mediaUrl);
                return;
            }

            // Find the bucket name start
            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = mediaUrl.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                log.warn("Cannot extract bucket name from URL: {}", mediaUrl);
                return;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = mediaUrl.substring(bucketEnd + 1);

            if (!fileKey.isEmpty()) {
                log.info("Deleting image from cloud storage. File Key: {}", fileKey);
                cloudStorageService.deleteImage(fileKey);
                log.info("Image deleted successfully from cloud storage");
            }
        } catch (Exception e) {
            log.warn("Failed to delete image from cloud storage. URL: {}", mediaUrl, e);
            // Don't throw exception - log and continue
        }
    }

    private PostResponseDTO mapToResponse(Post post) {
        Long likesCount = likeService.countByPostId(post.getId());
        Long commentsCount = (long) commentService.findAllByPostId(post.getId()).size();
        return PostTransformer.toDTO(post, likesCount, commentsCount);
    }
}
