package in.gram.gov.app.egram_service.controller.admin;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.request.OrderUpdateRequestDTO;
import in.gram.gov.app.egram_service.dto.request.PlatformSectionRequestDTO;
import in.gram.gov.app.egram_service.dto.request.VisibilityUpdateRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PlatformLandingPageConfigDTO;
import in.gram.gov.app.egram_service.dto.response.PlatformSectionResponseDTO;
import in.gram.gov.app.egram_service.facade.PlatformLandingPageFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/platform/landing-page")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Slf4j
public class AdminPlatformLandingPageController {
    private final PlatformLandingPageFacade facade;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformLandingPageConfigDTO>> getAllSections() {
        log.info("AdminPlatformLandingPageController.getAllSections called");
        PlatformLandingPageConfigDTO config = facade.getLandingPageConfig();
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping(value = "/sections", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PlatformSectionResponseDTO>> createSection(
            @RequestParam(required = false) String sectionType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String layoutType,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(required = false) String backgroundColor,
            @RequestParam(required = false) String textColor,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String imageKey,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) List<MultipartFile> contentItemImages) {
        
        log.info("AdminPlatformLandingPageController.createSection called");
        if (contentItemImages != null && !contentItemImages.isEmpty()) {
            log.info("Received {} content item images for platform section creation", contentItemImages.size());
        }
        
        PlatformSectionRequestDTO request = new PlatformSectionRequestDTO();
        if (sectionType != null) {
            request.setSectionType(in.gram.gov.app.egram_service.constants.enums.SectionType.valueOf(sectionType));
        }
        request.setTitle(title);
        request.setSubtitle(subtitle);
        request.setContent(content);
        if (layoutType != null) {
            request.setLayoutType(in.gram.gov.app.egram_service.constants.enums.LayoutType.valueOf(layoutType));
        }
        request.setDisplayOrder(displayOrder);
        request.setIsVisible(isVisible);
        request.setBackgroundColor(backgroundColor);
        request.setTextColor(textColor);
        request.setImageUrl(imageUrl);
        request.setImageKey(imageKey);
        request.setMetadata(metadata);
        request.setImageFile(imageFile);
        request.setContentItemImages(contentItemImages);
        try {
            request.setCompressionQuality(CompressionQuality.valueOf(compressionQuality.toUpperCase()));
        } catch (Exception e) {
            request.setCompressionQuality(CompressionQuality.HIGH);
        }

        PlatformSectionResponseDTO response = facade.createSection(request);
        return ResponseEntity.ok(ApiResponse.success("Section created successfully", response));
    }

    @PostMapping(value = "/sections", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PlatformSectionResponseDTO>> createSectionJson(
            @Valid @RequestBody PlatformSectionRequestDTO request) {
        log.info("AdminPlatformLandingPageController.createSectionJson called");
        PlatformSectionResponseDTO response = facade.createSection(request);
        return ResponseEntity.ok(ApiResponse.success("Section created successfully", response));
    }

    @PutMapping(value = "/sections/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PlatformSectionResponseDTO>> updateSection(
            @PathVariable Long id,
            @RequestParam(required = false) String sectionType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String layoutType,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(required = false) String backgroundColor,
            @RequestParam(required = false) String textColor,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String imageKey,
            @RequestParam(required = false) String metadata,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) List<MultipartFile> contentItemImages) {
        log.info("AdminPlatformLandingPageController.updateSection called - id={}", id);
        if (contentItemImages != null && !contentItemImages.isEmpty()) {
            log.info("Received {} content item images for platform section update", contentItemImages.size());
        }
        
        PlatformSectionRequestDTO request = new PlatformSectionRequestDTO();
        if (sectionType != null) {
            try {
                request.setSectionType(in.gram.gov.app.egram_service.constants.enums.SectionType.valueOf(sectionType));
            } catch (Exception e) {
                log.warn("Invalid sectionType: {}", sectionType);
            }
        }
        request.setTitle(title);
        request.setSubtitle(subtitle);
        request.setContent(content);
        if (layoutType != null) {
            try {
                request.setLayoutType(in.gram.gov.app.egram_service.constants.enums.LayoutType.valueOf(layoutType));
            } catch (Exception e) {
                log.warn("Invalid layoutType: {}", layoutType);
            }
        }
        request.setDisplayOrder(displayOrder);
        request.setIsVisible(isVisible);
        request.setBackgroundColor(backgroundColor);
        request.setTextColor(textColor);
        request.setImageUrl(imageUrl);
        request.setImageKey(imageKey);
        request.setMetadata(metadata);
        request.setImageFile(imageFile);
        request.setContentItemImages(contentItemImages);
        try {
            request.setCompressionQuality(CompressionQuality.valueOf(compressionQuality.toUpperCase()));
        } catch (Exception e) {
            request.setCompressionQuality(CompressionQuality.HIGH);
        }
        
        PlatformSectionResponseDTO response = facade.updateSection(id, request);
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", response));
    }

    @PutMapping(value = "/sections/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PlatformSectionResponseDTO>> updateSectionJson(
            @PathVariable Long id,
            @Valid @RequestBody PlatformSectionRequestDTO request) {
        log.info("AdminPlatformLandingPageController.updateSectionJson called - id={}", id);
        PlatformSectionResponseDTO response = facade.updateSection(id, request);
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", response));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteSection(@PathVariable Long id) {
        log.info("AdminPlatformLandingPageController.deleteSection called - id={}", id);
        facade.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", null));
    }

    @PatchMapping("/sections/{id}/order")
    public ResponseEntity<ApiResponse<Object>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequestDTO request) {
        log.info("AdminPlatformLandingPageController.updateOrder called - id={}, order={}", id, request.getDisplayOrder());
        facade.updateDisplayOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Display order updated successfully", null));
    }

    @PatchMapping("/sections/{id}/visibility")
    public ResponseEntity<ApiResponse<Object>> updateVisibility(
            @PathVariable Long id,
            @Valid @RequestBody VisibilityUpdateRequestDTO request) {
        log.info("AdminPlatformLandingPageController.updateVisibility called - id={}, isVisible={}", id, request.getIsVisible());
        facade.updateVisibility(id, request);
        return ResponseEntity.ok(ApiResponse.success("Visibility updated successfully", null));
    }

    @PostMapping(value = "/sections/{id}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PlatformSectionResponseDTO>> uploadImage(
            @PathVariable Long id,
            @RequestParam MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality) {
        log.info("AdminPlatformLandingPageController.uploadImage called - id={}", id);
        CompressionQuality quality;
        try {
            quality = CompressionQuality.valueOf(compressionQuality.toUpperCase());
        } catch (Exception e) {
            quality = CompressionQuality.HIGH;
        }
        PlatformSectionResponseDTO response = facade.uploadImage(id, imageFile, quality);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }

    /**
     * Generic image upload endpoint for content items
     * Uploads an image and returns only the image URL without associating it with a section
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImageGeneric(
            @RequestParam MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality) {
        log.info("AdminPlatformLandingPageController.uploadImageGeneric called");
        CompressionQuality quality;
        try {
            quality = CompressionQuality.valueOf(compressionQuality.toUpperCase());
        } catch (Exception e) {
            quality = CompressionQuality.HIGH;
        }
        String imageUrl = facade.uploadImageGeneric(imageFile, quality);
        ImageUploadResponse response = new ImageUploadResponse();
        response.setImageUrl(imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }

    /**
     * Simple response DTO for generic image upload
     */
    public static class ImageUploadResponse {
        private String imageUrl;
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}

