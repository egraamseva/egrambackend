package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.PanchayatWebsiteSection;
import in.gram.gov.app.egram_service.dto.request.OrderUpdateRequestDTO;
import in.gram.gov.app.egram_service.dto.request.PanchayatWebsiteSectionRequestDTO;
import in.gram.gov.app.egram_service.dto.request.VisibilityUpdateRequestDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteConfigDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteSectionResponseDTO;
import in.gram.gov.app.egram_service.service.CloudStorageService;
import in.gram.gov.app.egram_service.service.ImageCompressionService;
import in.gram.gov.app.egram_service.service.PanchayatService;
import in.gram.gov.app.egram_service.service.PanchayatWebsiteSectionService;
import in.gram.gov.app.egram_service.transformer.PanchayatWebsiteSectionTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PanchayatWebsiteFacade {
    private final PanchayatWebsiteSectionService sectionService;
    private final PanchayatService panchayatService;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;

    public List<PanchayatWebsiteSectionResponseDTO> getAllSections() {
        log.info("PanchayatWebsiteFacade.getAllSections called");
        Long panchayatId = TenantContext.getTenantId();
        List<PanchayatWebsiteSection> sections = sectionService.findByPanchayatId(panchayatId);
        return sections.stream()
                .map(PanchayatWebsiteSectionTransformer::toDTO)
                .collect(Collectors.toList());
    }

    public PanchayatWebsiteConfigDTO getWebsiteConfig() {
        log.info("PanchayatWebsiteFacade.getWebsiteConfig called");
        List<PanchayatWebsiteSectionResponseDTO> sections = getAllSections();
        PanchayatWebsiteConfigDTO config = new PanchayatWebsiteConfigDTO();
        config.setSections(sections);
        return config;
    }

    public List<PanchayatWebsiteSectionResponseDTO> getVisibleSections(String slug) {
        log.info("PanchayatWebsiteFacade.getVisibleSections called - slug={}", slug);
        List<PanchayatWebsiteSection> sections = sectionService.findByPanchayatSlugAndVisible(slug);
        return sections.stream()
                .map(PanchayatWebsiteSectionTransformer::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PanchayatWebsiteSectionResponseDTO createSection(PanchayatWebsiteSectionRequestDTO request) {
        log.info("PanchayatWebsiteFacade.createSection called - sectionType={}", request.getSectionType());
        
        // Validate required fields for create operation
        if (request.getSectionType() == null) {
            throw new IllegalArgumentException("Section type is required");
        }
        if (request.getLayoutType() == null) {
            throw new IllegalArgumentException("Layout type is required");
        }
        
        Long panchayatId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(panchayatId);

        // Handle image upload if provided
        String imageUrl = request.getImageUrl();
        String imageKey = request.getImageKey();
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            ImageCompressionDTO uploadResult = processAndUploadImage(
                    request.getImageFile(), 
                    request.getCompressionQuality()
            );
            if (uploadResult != null) {
                imageUrl = uploadResult.getBackblazeFileUrl();
                imageKey = extractFileKeyFromUrl(imageUrl);
            }
        }

        PanchayatWebsiteSection section = PanchayatWebsiteSectionTransformer.toEntity(request, panchayat);
        section.setImageUrl(imageUrl);
        section.setImageKey(imageKey);
        
        section = sectionService.create(section);
        log.info("Panchayat website section created successfully with ID: {}", section.getId());
        return PanchayatWebsiteSectionTransformer.toDTO(section);
    }

    public PanchayatWebsiteSectionResponseDTO getSectionById(Long id) {
        log.debug("PanchayatWebsiteFacade.getSectionById called - id={}", id);
        PanchayatWebsiteSection section = sectionService.findById(id);
        // Verify it belongs to current panchayat
        Long panchayatId = TenantContext.getTenantId();
        if (!section.getPanchayat().getId().equals(panchayatId)) {
            throw new RuntimeException("Section does not belong to current panchayat");
        }
        return PanchayatWebsiteSectionTransformer.toDTO(section);
    }

    @Transactional
    public PanchayatWebsiteSectionResponseDTO updateSection(Long id, PanchayatWebsiteSectionRequestDTO request) {
        log.info("PanchayatWebsiteFacade.updateSection called - id={}", id);
        PanchayatWebsiteSection section = sectionService.findById(id);
        
        // Verify it belongs to current panchayat
        Long panchayatId = TenantContext.getTenantId();
        if (!section.getPanchayat().getId().equals(panchayatId)) {
            throw new RuntimeException("Section does not belong to current panchayat");
        }

        // Update fields
        if (request.getSectionType() != null) {
            section.setSectionType(request.getSectionType());
        }
        if (request.getTitle() != null) {
            section.setTitle(request.getTitle());
        }
        if (request.getSubtitle() != null) {
            section.setSubtitle(request.getSubtitle());
        }
        if (request.getContent() != null) {
            section.setContent(request.getContent());
        }
        if (request.getLayoutType() != null) {
            section.setLayoutType(request.getLayoutType());
        }
        if (request.getDisplayOrder() != null) {
            section.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsVisible() != null) {
            section.setIsVisible(request.getIsVisible());
        }
        if (request.getBackgroundColor() != null) {
            section.setBackgroundColor(request.getBackgroundColor());
        }
        if (request.getTextColor() != null) {
            section.setTextColor(request.getTextColor());
        }
        if (request.getMetadata() != null) {
            section.setMetadata(request.getMetadata());
        }

        // Handle image upload if new image provided
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            // Delete old image if exists
            if (section.getImageKey() != null && cloudStorageService.isEnabled()) {
                try {
                    cloudStorageService.deleteImage(section.getImageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }

            ImageCompressionDTO uploadResult = processAndUploadImage(
                    request.getImageFile(), 
                    request.getCompressionQuality()
            );
            if (uploadResult != null) {
                section.setImageUrl(uploadResult.getBackblazeFileUrl());
                section.setImageKey(extractFileKeyFromUrl(uploadResult.getBackblazeFileUrl()));
            }
        } else if (request.getImageUrl() != null) {
            section.setImageUrl(request.getImageUrl());
            section.setImageKey(request.getImageKey());
        }

        section = sectionService.update(section);
        log.info("Panchayat website section updated successfully with ID: {}", id);
        return PanchayatWebsiteSectionTransformer.toDTO(section);
    }

    @Transactional
    public void deleteSection(Long id) {
        log.info("PanchayatWebsiteFacade.deleteSection called - id={}", id);
        PanchayatWebsiteSection section = sectionService.findById(id);
        
        // Verify it belongs to current panchayat
        Long panchayatId = TenantContext.getTenantId();
        if (!section.getPanchayat().getId().equals(panchayatId)) {
            throw new RuntimeException("Section does not belong to current panchayat");
        }

        // Delete image from cloud storage if exists
        if (section.getImageKey() != null && cloudStorageService.isEnabled()) {
            try {
                cloudStorageService.deleteImage(section.getImageKey());
            } catch (Exception e) {
                log.warn("Failed to delete image from cloud storage: {}", e.getMessage());
            }
        }

        sectionService.delete(id);
        log.info("Panchayat website section deleted successfully with ID: {}", id);
    }

    @Transactional
    public void updateDisplayOrder(Long id, OrderUpdateRequestDTO request) {
        log.info("PanchayatWebsiteFacade.updateDisplayOrder called - id={}, order={}", id, request.getDisplayOrder());
        PanchayatWebsiteSection section = sectionService.findById(id);
        
        // Verify it belongs to current panchayat
        Long panchayatId = TenantContext.getTenantId();
        if (!section.getPanchayat().getId().equals(panchayatId)) {
            throw new RuntimeException("Section does not belong to current panchayat");
        }
        
        sectionService.updateDisplayOrder(id, request.getDisplayOrder());
    }

    @Transactional
    public void updateVisibility(Long id, VisibilityUpdateRequestDTO request) {
        log.info("PanchayatWebsiteFacade.updateVisibility called - id={}, isVisible={}", id, request.getIsVisible());
        PanchayatWebsiteSection section = sectionService.findById(id);
        
        // Verify it belongs to current panchayat
        Long panchayatId = TenantContext.getTenantId();
        if (!section.getPanchayat().getId().equals(panchayatId)) {
            throw new RuntimeException("Section does not belong to current panchayat");
        }
        
        sectionService.updateVisibility(id, request.getIsVisible());
    }

    @Transactional
    public PanchayatWebsiteSectionResponseDTO uploadImage(Long id, MultipartFile imageFile, CompressionQuality compressionQuality) {
        log.info("PanchayatWebsiteFacade.uploadImage called - id={}", id);
        PanchayatWebsiteSection section = sectionService.findById(id);
        
        // Verify it belongs to current panchayat
        Long panchayatId = TenantContext.getTenantId();
        if (!section.getPanchayat().getId().equals(panchayatId)) {
            throw new RuntimeException("Section does not belong to current panchayat");
        }

        // Delete old image if exists
        if (section.getImageKey() != null && cloudStorageService.isEnabled()) {
            try {
                cloudStorageService.deleteImage(section.getImageKey());
            } catch (Exception e) {
                log.warn("Failed to delete old image: {}", e.getMessage());
            }
        }

        ImageCompressionDTO uploadResult = processAndUploadImage(imageFile, compressionQuality);
        if (uploadResult != null) {
            section.setImageUrl(uploadResult.getBackblazeFileUrl());
            section.setImageKey(extractFileKeyFromUrl(uploadResult.getBackblazeFileUrl()));
            section = sectionService.update(section);
        }

        return PanchayatWebsiteSectionTransformer.toDTO(section);
    }

    private ImageCompressionDTO processAndUploadImage(MultipartFile imageFile, CompressionQuality compressionQuality) {
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
            return uploadResult;

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
     * @param mediaUrl The presigned URL or file key
     * @return Extracted file key or null if not a URL or cannot extract
     */
    private String extractFileKeyFromUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            return null;
        }

        // If it's not a URL (doesn't start with http:// or https://), assume it's already a file key
        if (!mediaUrl.startsWith("http://") && !mediaUrl.startsWith("https://")) {
            return null; // Already a file key, return null so caller uses it as-is
        }

        try {
            String fileKey = null;

            // Handle Backblaze B2 file URLs: https://f001.backblazeb2.com/file/bucket-name/images/...
            if (mediaUrl.contains("/file/")) {
                int fileIndex = mediaUrl.indexOf("/file/");
                int bucketStart = fileIndex + 6; // "/file/" length
                int bucketEnd = mediaUrl.indexOf("/", bucketStart);
                if (bucketEnd != -1) {
                    fileKey = mediaUrl.substring(bucketEnd + 1);
                }
            }
            // Handle S3 direct URLs: https://bucket.s3.region.backblazeb2.com/images/...
            // Example: https://egraamseva.s3.us-east-005.backblazeb2.com/images/1764829153364-3aa4a7ee.jpg
            else if (mediaUrl.contains(".s3.") && mediaUrl.contains(".backblazeb2.com")) {
                // Find the domain part and extract path after it
                int s3Index = mediaUrl.indexOf(".s3.");
                if (s3Index != -1) {
                    // Find the end of domain (first / after .s3.)
                    int domainEnd = mediaUrl.indexOf("/", s3Index);
                    if (domainEnd != -1) {
                        fileKey = mediaUrl.substring(domainEnd + 1);
                    } else {
                        // No path, return null
                        return null;
                    }
                }
            }
            // Handle generic S3 URLs: https://bucket.s3.amazonaws.com/images/...
            else if (mediaUrl.contains(".s3.") || mediaUrl.contains("s3.amazonaws.com")) {
                try {
                    java.net.URL urlObj = new java.net.URL(mediaUrl);
                    String path = urlObj.getPath();
                    if (path != null && !path.isEmpty()) {
                        // Remove leading slash
                        fileKey = path.startsWith("/") ? path.substring(1) : path;
                    }
                } catch (java.net.MalformedURLException e) {
                    log.warn("Malformed URL: {}", mediaUrl, e);
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
            log.error("Error extracting file key from URL: {}", mediaUrl, e);
            return null;
        }
    }
}

