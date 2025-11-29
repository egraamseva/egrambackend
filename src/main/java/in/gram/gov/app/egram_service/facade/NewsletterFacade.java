package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Newsletter;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.dto.request.NewsletterRequestDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.dto.response.NewsletterResponseDTO;
import in.gram.gov.app.egram_service.service.*;
import in.gram.gov.app.egram_service.transformer.NewsletterTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterFacade {
    private final NewsletterService newsletterService;
    private final PanchayatService panchayatService;
    private final UserService userService;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;
    private final S3CloudStorageService s3CloudStorageService;

    /**
     * Create a new newsletter with optional cover image upload
     */
    @Transactional
    public NewsletterResponseDTO create(NewsletterRequestDTO request, String email) {
        log.info("NewsletterFacade.create called - title={}, email={}", request.getTitle(), email);
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);
        User author = userService.findByEmail(email);

        // Handle cover image upload if provided
        String coverImageFileKey = request.getCoverImageFileKey();
        if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
            coverImageFileKey = processAndUploadCoverImage(request.getCoverImageFile(), request.getCompressionQuality());
        }

        Newsletter newsletter = Newsletter.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .coverImageFileKey(coverImageFileKey)
                .content(request.getContent())
                .bulletPoints(request.getBulletPoints() != null ? request.getBulletPoints() : new java.util.ArrayList<>())
                .publishedOn(request.getPublishedOn())
                .authorName(request.getAuthorName() != null ? request.getAuthorName() : author.getName())
                .attachments(request.getAttachments() != null ? request.getAttachments() : new java.util.ArrayList<>())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .panchayat(panchayat)
                .createdBy(author)
                .build();

        // Set publishedOn if publishing
        if (newsletter.getIsPublished() && newsletter.getPublishedOn() == null) {
            newsletter.setPublishedOn(LocalDate.now());
        }

        newsletter = newsletterService.create(newsletter);
        log.info("Newsletter created successfully with ID: {}", newsletter.getId());
        return mapToResponse(newsletter);
    }

    public NewsletterResponseDTO getById(Long id) {
        log.debug("NewsletterFacade.getById called - id={}", id);
        Newsletter newsletter = newsletterService.findById(id);
        return mapToResponse(newsletter);
    }

    public NewsletterResponseDTO getPublishedByIdAndSlug(Long newsletterId, String slug) {
        log.info("NewsletterFacade.getPublishedByIdAndSlug called - newsletterId={}, slug={}", newsletterId, slug);
        Newsletter newsletter = newsletterService.findPublishedByIdAndSlug(newsletterId, slug);
        return mapToResponse(newsletter);
    }

    public Page<NewsletterResponseDTO> getAll(Pageable pageable) {
        log.info("NewsletterFacade.getAll called - pageable={}", pageable);
        Long tenantId = TenantContext.getTenantId();
        Page<Newsletter> newsletters = newsletterService.findByPanchayatId(tenantId, pageable);
        return newsletters.map(this::mapToResponse);
    }

    public Page<NewsletterResponseDTO> getPublishedBySlug(String slug, String search, Pageable pageable) {
        log.info("NewsletterFacade.getPublishedBySlug called - slug={}, search={}, pageable={}", slug, search, pageable);
        Page<Newsletter> newsletters = newsletterService.findPublishedBySlugWithSearch(slug, search, pageable);
        return newsletters.map(this::mapToResponse);
    }

    /**
     * Update a newsletter with optional new cover image
     */
    @Transactional
    public NewsletterResponseDTO update(Long id, NewsletterRequestDTO request) {
        log.info("NewsletterFacade.update called - id={}", id);
        Newsletter newsletter = newsletterService.findById(id);

        newsletter.setTitle(request.getTitle());
        newsletter.setSubtitle(request.getSubtitle());
        newsletter.setContent(request.getContent());
        newsletter.setBulletPoints(request.getBulletPoints() != null ? request.getBulletPoints() : new java.util.ArrayList<>());
        newsletter.setPublishedOn(request.getPublishedOn());
        newsletter.setAuthorName(request.getAuthorName());
        newsletter.setAttachments(request.getAttachments() != null ? request.getAttachments() : new java.util.ArrayList<>());

        // Handle cover image update if new image provided
        if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
            // Delete old image if exists
            if (newsletter.getCoverImageFileKey() != null && cloudStorageService.isEnabled()) {
                deleteImageFile(newsletter.getCoverImageFileKey());
            }
            // Upload new image
            String newCoverImageFileKey = processAndUploadCoverImage(request.getCoverImageFile(), request.getCompressionQuality());
            newsletter.setCoverImageFileKey(newCoverImageFileKey);
        } else if (request.getCoverImageFileKey() != null) {
            // Update with provided file key if no new file
            newsletter.setCoverImageFileKey(request.getCoverImageFileKey());
        }

        // Update published status
        if (request.getIsPublished() != null) {
            newsletter.setIsPublished(request.getIsPublished());
            if (newsletter.getIsPublished() && newsletter.getPublishedOn() == null) {
                newsletter.setPublishedOn(LocalDate.now());
            }
        }

        newsletter = newsletterService.update(newsletter);
        log.info("Newsletter updated successfully with ID: {}", id);
        return mapToResponse(newsletter);
    }

    @Transactional
    public void togglePublish(Long id) {
        log.info("NewsletterFacade.togglePublish called - id={}", id);
        newsletterService.togglePublish(id);
    }

    @Transactional
    public void delete(Long id) {
        log.info("NewsletterFacade.delete called - id={}", id);
        Newsletter newsletter = newsletterService.findById(id);
        
        // Delete cover image if exists
        if (newsletter.getCoverImageFileKey() != null && cloudStorageService.isEnabled()) {
            deleteImageFile(newsletter.getCoverImageFileKey());
        }
        
        newsletterService.delete(id);
    }

    /**
     * Process and upload cover image
     * Returns the file key (not the presigned URL) for storage in DB
     */
    private String processAndUploadCoverImage(MultipartFile imageFile, CompressionQuality compressionQuality) {
        try {
            if (!cloudStorageService.isEnabled()) {
                log.warn("Cloud storage is disabled. Image upload skipped");
                return null;
            }

            if (compressionQuality == null) {
                compressionQuality = CompressionQuality.HIGH;
            }

            log.info("Processing cover image for upload: {}", imageFile.getOriginalFilename());

            // Step 1: Validate image
            imageCompressionService.validateImageFile(imageFile);

            // Step 2: Compress image
            ImageCompressionDTO compressionMetadata = imageCompressionService.compressImage(
                    imageFile,
                    compressionQuality
            );

            log.info("Cover image compressed successfully. Original: {} bytes, Compressed: {} bytes",
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

            log.info("Cover image uploaded to cloud storage. File Key: {}", uploadResult.getBackblazeFileId());
            
            // Return file key (not presigned URL) for storage
            return uploadResult.getBackblazeFileId();

        } catch (Exception e) {
            log.error("Error processing cover image for upload", e);
            // Don't fail the entire newsletter creation if image upload fails
            return null;
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL
     */
    private String extractFileKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            int fileIndex = imageUrl.indexOf("/file/");
            if (fileIndex == -1) {
                log.warn("Invalid image URL format - '/file/' not found: {}", imageUrl);
                return null;
            }

            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = imageUrl.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                log.warn("Cannot extract bucket name from URL: {}", imageUrl);
                return null;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = imageUrl.substring(bucketEnd + 1);

            if (fileKey.isEmpty()) {
                log.warn("Extracted file key is empty from URL: {}", imageUrl);
                return null;
            }

            // Remove query parameters if present (from presigned URLs)
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

    /**
     * Delete image from cloud storage
     */
    private void deleteImageFile(String fileKey) {
        try {
            if (fileKey == null || fileKey.isEmpty()) {
                return;
            }

            // If it's a URL, extract the file key
            String actualFileKey = fileKey;
            if (fileKey.contains("/file/")) {
                actualFileKey = extractFileKeyFromUrl(fileKey);
            }

            if (actualFileKey != null && !actualFileKey.isEmpty()) {
                log.info("Deleting cover image from cloud storage. File Key: {}", actualFileKey);
                cloudStorageService.deleteImage(actualFileKey);
                log.info("Cover image deleted successfully from cloud storage");
            }
        } catch (Exception e) {
            log.warn("Failed to delete cover image from cloud storage. File Key: {}", fileKey, e);
            // Don't throw exception - log and continue
        }
    }

    /**
     * Map Newsletter entity to DTO
     */
    private NewsletterResponseDTO mapToResponse(Newsletter newsletter) {
        return NewsletterTransformer.toDTO(newsletter, s3CloudStorageService);
    }
}

