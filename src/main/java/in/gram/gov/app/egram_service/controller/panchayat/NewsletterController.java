package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.request.NewsletterRequestDTO;
import in.gram.gov.app.egram_service.dto.response.NewsletterResponseDTO;
import in.gram.gov.app.egram_service.facade.NewsletterFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Newsletter Controller (Protected - Sachiv/Admin only)
 * Manages newsletter CRUD operations
 */
@RestController
@RequestMapping("/api/v1/panchayat/newsletters")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
public class NewsletterController {
    private final NewsletterFacade newsletterFacade;

    /**
     * Create a new newsletter with optional cover image upload
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<NewsletterResponseDTO>> create(
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String bulletPoints, // JSON array as string
            @RequestParam(required = false) String publishedOn, // ISO date string
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) String attachments, // JSON array as string
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) MultipartFile coverImageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String coverImageFileKey,
            Authentication authentication) {

        String email = authentication.getName();

        // Build NewsletterRequestDTO from form parameters
        NewsletterRequestDTO request = new NewsletterRequestDTO();
        request.setTitle(title);
        request.setSubtitle(subtitle);
        request.setContent(content);
        request.setCoverImageFile(coverImageFile);
        request.setCoverImageFileKey(coverImageFileKey);
        request.setAuthorName(authorName);
        request.setIsPublished(isPublished != null ? isPublished : false);

        try {
            request.setCompressionQuality(CompressionQuality.valueOf(compressionQuality.toUpperCase()));
        } catch (IllegalArgumentException e) {
            request.setCompressionQuality(CompressionQuality.HIGH);
        }

        // Parse bulletPoints JSON array
        if (bulletPoints != null && !bulletPoints.isEmpty()) {
            try {
                request.setBulletPoints(java.util.Arrays.asList(
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(bulletPoints, String[].class)
                ));
            } catch (Exception e) {
                log.warn("Failed to parse bulletPoints JSON: {}", bulletPoints, e);
            }
        }

        // Parse attachments JSON array
        if (attachments != null && !attachments.isEmpty()) {
            try {
                request.setAttachments(java.util.Arrays.asList(
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(attachments, String[].class)
                ));
            } catch (Exception e) {
                log.warn("Failed to parse attachments JSON: {}", attachments, e);
            }
        }

        // Parse publishedOn date
        if (publishedOn != null && !publishedOn.isEmpty()) {
            try {
                request.setPublishedOn(java.time.LocalDate.parse(publishedOn));
            } catch (Exception e) {
                log.warn("Failed to parse publishedOn date: {}", publishedOn, e);
            }
        }

        log.info("Creating newsletter for user: {}, with cover image upload: {}",
                email, coverImageFile != null && !coverImageFile.isEmpty());

        NewsletterResponseDTO response = newsletterFacade.create(request, email);
        return ResponseEntity.ok(ApiResponse.success("Newsletter created successfully", response));
    }

    /**
     * Get all newsletters for the current panchayat (drafts and published)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NewsletterResponseDTO>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt",direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all newsletters");
        PagedResponse<NewsletterResponseDTO> response = PagedResponse.of(newsletterFacade.getAll(pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get newsletter by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsletterResponseDTO>> getById(@PathVariable Long id) {
        log.info("Fetching newsletter by ID: {}", id);
        NewsletterResponseDTO response = newsletterFacade.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update existing newsletter with optional new cover image
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<NewsletterResponseDTO>> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String bulletPoints,
            @RequestParam(required = false) String publishedOn,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) String attachments,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) MultipartFile coverImageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String coverImageFileKey) {

        // Build NewsletterRequestDTO from form parameters
        NewsletterRequestDTO request = new NewsletterRequestDTO();
        request.setTitle(title);
        request.setSubtitle(subtitle);
        request.setContent(content);
        request.setCoverImageFile(coverImageFile);
        request.setCoverImageFileKey(coverImageFileKey);
        request.setAuthorName(authorName);
        request.setIsPublished(isPublished);

        try {
            request.setCompressionQuality(CompressionQuality.valueOf(compressionQuality.toUpperCase()));
        } catch (IllegalArgumentException e) {
            request.setCompressionQuality(CompressionQuality.HIGH);
        }

        // Parse bulletPoints JSON array
        if (bulletPoints != null && !bulletPoints.isEmpty()) {
            try {
                request.setBulletPoints(java.util.Arrays.asList(
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(bulletPoints, String[].class)
                ));
            } catch (Exception e) {
                log.warn("Failed to parse bulletPoints JSON: {}", bulletPoints, e);
            }
        }

        // Parse attachments JSON array
        if (attachments != null && !attachments.isEmpty()) {
            try {
                request.setAttachments(java.util.Arrays.asList(
                    new com.fasterxml.jackson.databind.ObjectMapper().readValue(attachments, String[].class)
                ));
            } catch (Exception e) {
                log.warn("Failed to parse attachments JSON: {}", attachments, e);
            }
        }

        // Parse publishedOn date
        if (publishedOn != null && !publishedOn.isEmpty()) {
            try {
                request.setPublishedOn(java.time.LocalDate.parse(publishedOn));
            } catch (Exception e) {
                log.warn("Failed to parse publishedOn date: {}", publishedOn, e);
            }
        }

        log.info("Updating newsletter with ID: {}", id);
        NewsletterResponseDTO response = newsletterFacade.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Newsletter updated successfully", response));
    }

    /**
     * Toggle publish/unpublish status
     */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> togglePublish(@PathVariable Long id) {
        log.info("Toggling publish status for newsletter ID: {}", id);
        newsletterFacade.togglePublish(id);
        return ResponseEntity.ok(ApiResponse.success("Newsletter publish status updated", null));
    }

    /**
     * Delete newsletter
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Deleting newsletter with ID: {}", id);
        newsletterFacade.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Newsletter deleted successfully", null));
    }
}

