package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.enums.PanchayatStatus;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.dto.filters.PanchayatFilter;
import in.gram.gov.app.egram_service.dto.request.PanchayatRequestDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatResponseDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatStatsResponseDTO;
import in.gram.gov.app.egram_service.service.*;
import in.gram.gov.app.egram_service.transformer.PanchayatTransformer;
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
public class PanchayatFacade {
    private final PanchayatService panchayatService;
    private final UserService userService;
    private final PostService postService;
    private final AnnouncementService announcementService;
    private final SchemeService schemeService;
    private final DocumentService documentService;
    private final GalleryImageService galleryImageService;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;

    @Transactional
    public PanchayatResponseDTO create(PanchayatRequestDTO request) {
        log.info("PanchayatFacade.create called - slug={}", request.getSlug());
        Panchayat panchayat = PanchayatTransformer.toEntity(request);
        panchayat = panchayatService.create(panchayat);
        return PanchayatTransformer.toDTO(panchayat);
    }

    public PanchayatResponseDTO getById(Long id) {
        log.debug("PanchayatFacade.getById called - id={}", id);
        Panchayat panchayat = panchayatService.findById(id);
        return PanchayatTransformer.toDTO(panchayat);
    }

    public PanchayatResponseDTO getBySlug(String slug) {
        log.debug("PanchayatFacade.getBySlug called - slug={}", slug);
        Panchayat panchayat = panchayatService.findBySlug(slug);
        return PanchayatTransformer.toDTO(panchayat);
    }

    public Page<PanchayatResponseDTO> getAll(PanchayatFilter panchayatFilter) {
        log.info("PanchayatFacade.getAll called - filter={}", panchayatFilter);
        Page<Panchayat> panchayats = panchayatService.findByFilters(panchayatFilter);
        return panchayats.map(PanchayatTransformer::toDTO);
    }

    @Transactional
    public PanchayatResponseDTO update(Long id, PanchayatRequestDTO request) {
        log.info("PanchayatFacade.update called - id={}", id);
        Panchayat panchayat = panchayatService.findById(id);
        PanchayatTransformer.updateEntity(panchayat, request);
        panchayat = panchayatService.update(panchayat);
        return PanchayatTransformer.toDTO(panchayat);
    }

    @Transactional
    public void updateStatus(Long id, PanchayatStatus status) {
        log.info("PanchayatFacade.updateStatus called - id={}, status={}", id, status);
        panchayatService.updateStatus(id, status);
    }

    @Transactional
    public void delete(Long id) {
        log.info("PanchayatFacade.delete called - id={}", id);
        // Mark panchayat as inactive
        panchayatService.delete(id);
        // Asynchronously deactivate all users of this panchayat
        userService.deactivateUsersByPanchayatIdAsync(id);
        log.info("Panchayat {} marked as inactive and user deactivation process started", id);
    }

    public PanchayatStatsResponseDTO getStats(Long id) {
        log.info("PanchayatFacade.getStats called - id={}", id);
        PanchayatStatsResponseDTO stats = new PanchayatStatsResponseDTO();
        stats.setTotalUsers(userService.findByPanchayatId(id, Pageable.unpaged()).getTotalElements());
        stats.setTotalPosts(postService.findByPanchayatId(id, Pageable.unpaged()).getTotalElements());
        stats.setTotalAnnouncements(announcementService.findByPanchayatId(id, Pageable.unpaged()).getTotalElements());
        stats.setTotalSchemes(schemeService.findByPanchayatId(id, Pageable.unpaged()).getTotalElements());
        stats.setTotalDocuments(documentService.findByPanchayatId(id, Pageable.unpaged()).getTotalElements());
        stats.setTotalGalleryImages(galleryImageService.findByPanchayatId(id, Pageable.unpaged()).getTotalElements());
        return stats;
    }

    public PanchayatResponseDTO getCurrentPanchayat() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("No tenant context found");
        }
        return getById(tenantId);
    }

    @Transactional
    public PanchayatResponseDTO updateCurrent(PanchayatRequestDTO request) {
        Long tenantId = TenantContext.getTenantId();
        return update(tenantId, request);
    }

    @Transactional
    public PanchayatResponseDTO uploadHeroImage(MultipartFile imageFile, CompressionQuality compressionQuality) {
        log.info("PanchayatFacade.uploadHeroImage called");
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);

        // Delete old hero image if exists
        if (panchayat.getHeroImageUrl() != null && cloudStorageService.isEnabled()) {
            try {
                String fileKey = extractFileKeyFromUrl(panchayat.getHeroImageUrl());
                if (fileKey != null && !fileKey.isEmpty()) {
                    cloudStorageService.deleteImage(fileKey);
                }
            } catch (Exception e) {
                log.warn("Failed to delete old hero image: {}", e.getMessage());
            }
        }

        // Process and upload new image
        String imageUrl = processAndUploadImage(imageFile, compressionQuality);
        if (imageUrl != null) {
            panchayat.setHeroImageUrl(imageUrl);
            panchayat = panchayatService.update(panchayat);
            log.info("Hero image uploaded successfully for panchayat ID: {}", tenantId);
        } else {
            log.warn("Hero image upload failed or cloud storage is disabled");
        }

        return PanchayatTransformer.toDTO(panchayat);
    }

    @Transactional
    public PanchayatResponseDTO uploadLogo(MultipartFile imageFile, CompressionQuality compressionQuality) {
        log.info("PanchayatFacade.uploadLogo called");
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);

        // Delete old logo if exists
        if (panchayat.getLogoUrl() != null && cloudStorageService.isEnabled()) {
            try {
                String fileKey = extractFileKeyFromUrl(panchayat.getLogoUrl());
                if (fileKey != null && !fileKey.isEmpty()) {
                    cloudStorageService.deleteImage(fileKey);
                }
            } catch (Exception e) {
                log.warn("Failed to delete old logo: {}", e.getMessage());
            }
        }

        // Process and upload new image
        String imageUrl = processAndUploadImage(imageFile, compressionQuality);
        if (imageUrl != null) {
            panchayat.setLogoUrl(imageUrl);
            panchayat = panchayatService.update(panchayat);
            log.info("Logo uploaded successfully for panchayat ID: {}", tenantId);
        } else {
            log.warn("Logo upload failed or cloud storage is disabled");
        }

        return PanchayatTransformer.toDTO(panchayat);
    }

    /**
     * Process image: compress and upload to cloud storage
     *
     * @param imageFile          MultipartFile to process
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

            // Step 4: Upload to cloud storage
            ImageCompressionDTO uploadResult = cloudStorageService.uploadImage(
                    compressedImageStream,
                    compressionMetadata
            );

            log.info("Image uploaded to cloud storage. URL: {}", uploadResult.getBackblazeFileUrl());
            return uploadResult.getBackblazeFileUrl();

        } catch (Exception e) {
            log.error("Error processing image for upload", e);
            return null;
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL or S3 direct URL
     * Supported URL formats:
     * 1. Backblaze B2 file URL: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext?authorization=...
     * 2. S3 direct URL: https://bucket.s3.region.backblazeb2.com/images/timestamp-uuid.ext?X-Amz-...
     * File key format: images/timestamp-uuid.ext
     *
     * @param imageUrl The presigned URL or file key
     * @return Extracted file key or null if not a URL or cannot extract
     */
    private String extractFileKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            // If it's already a file key (no http/https), return as is
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                return imageUrl;
            }

            // Extract file key from URL
            // Format: https://domain/path/to/file.ext?params
            int queryIndex = imageUrl.indexOf('?');
            String urlWithoutQuery = queryIndex > 0 ? imageUrl.substring(0, queryIndex) : imageUrl;

            // Find the path after the domain
            int pathStart = urlWithoutQuery.indexOf("/", 8); // Skip https://
            if (pathStart == -1) {
                return null;
            }

            String path = urlWithoutQuery.substring(pathStart + 1);

            // Remove bucket name if present (first segment)
            int firstSlash = path.indexOf('/');
            if (firstSlash > 0) {
                path = path.substring(firstSlash + 1);
            }

            return path.isEmpty() ? null : path;

        } catch (Exception e) {
            log.warn("Failed to extract file key from URL: {}", imageUrl, e);
            return null;
        }
    }
}
