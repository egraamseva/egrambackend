package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.filters.GalleryFilter;
import in.gram.gov.app.egram_service.dto.request.GalleryImageRequestDTO;
import in.gram.gov.app.egram_service.dto.response.GalleryImageResponseDTO;
import in.gram.gov.app.egram_service.facade.GalleryImageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Panchayat Gallery Controller
 * Manages gallery image operations including upload, retrieval, update, and deletion
 * Supports both direct file upload with compression and URL-only workflows
 */
@RestController
@RequestMapping("/api/v1/panchayat/gallery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
@Slf4j
public class PanchayatGalleryController {
    private final GalleryImageFacade galleryImageFacade;

    /**
     * Create gallery image with optional file upload
     * Accepts both form data with image file and JSON payload for URL-only workflow
     * @param caption Image caption
     * @param tags Image tags
     * @param albumId Album ID to associate with
     * @param displayOrder Display order within album
     * @param imageFile Optional image file to upload and compress
     * @param compressionQuality Compression quality for image (HIGH, MEDIUM, LOW)
     * @param authentication Current user authentication
     * @return Created gallery image with URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GalleryImageResponseDTO>> create(
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String imageUrl,
            Authentication authentication) {

        String email = authentication.getName();

        // Build GalleryImageRequestDTO from form parameters
        GalleryImageRequestDTO request = new GalleryImageRequestDTO();
        request.setCaption(caption);
        request.setTags(tags);
        request.setAlbumId(albumId);
        request.setDisplayOrder(displayOrder);
        request.setImageFile(imageFile);
        request.setImageUrl(imageUrl);

        try {
            request.setCompressionQuality(compressionQuality.toUpperCase());
        } catch (Exception e) {
            request.setCompressionQuality("HIGH");
        }

        log.info("Creating gallery image for user: {}, with image upload: {}",
                email, imageFile != null && !imageFile.isEmpty());

        GalleryImageResponseDTO response = galleryImageFacade.create(request, email);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }

    /**
     * Get all gallery images with optional album filter
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param albumId Optional album ID filter
     * @return Paginated list of gallery images
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<GalleryImageResponseDTO>>> getAll(
            GalleryFilter galleryFilter) {
        Page<GalleryImageResponseDTO> images = galleryImageFacade.getAll(galleryFilter);
        PagedResponse<GalleryImageResponseDTO> response = PagedResponse.of(images);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get gallery image by ID
     * @param id Gallery image ID
     * @return Gallery image details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GalleryImageResponseDTO>> getById(@PathVariable Long id) {
        GalleryImageResponseDTO response = galleryImageFacade.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update gallery image with optional new image file
     * Accepts both form data with image file and JSON payload
     * If new image provided: compress, upload to B2, delete old image, and update URL
     * @param id Gallery image ID
     * @param caption Updated caption
     * @param tags Updated tags
     * @param albumId Updated album association
     * @param displayOrder Updated display order
     * @param imageFile Optional new image file to upload
     * @param compressionQuality Compression quality for new image
     * @param imageUrl Optional new image URL
     * @return Updated gallery image
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GalleryImageResponseDTO>> update(
            @PathVariable Long id,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String imageUrl) {

        // Build GalleryImageRequestDTO from form parameters
        GalleryImageRequestDTO request = new GalleryImageRequestDTO();
        request.setCaption(caption);
        request.setTags(tags);
        request.setAlbumId(albumId);
        request.setDisplayOrder(displayOrder);
        request.setImageFile(imageFile);
        request.setImageUrl(imageUrl);

        try {
            request.setCompressionQuality(compressionQuality.toUpperCase());
        } catch (Exception e) {
            request.setCompressionQuality("HIGH");
        }

        log.info("Updating gallery image ID: {}, with new image: {}",
                id, imageFile != null && !imageFile.isEmpty());

        GalleryImageResponseDTO response = galleryImageFacade.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Image updated successfully", response));
    }

    /**
     * Refresh presigned URL for gallery image when expired
     * Call this endpoint when the image URL returns 403 Forbidden
     * @param id Gallery image ID
     * @return Updated gallery image with fresh presigned URL
     */
    @PatchMapping("/{id}/refresh-image-url")
    public ResponseEntity<ApiResponse<GalleryImageResponseDTO>> refreshImageUrl(@PathVariable Long id) {
        log.info("Refreshing image URL for gallery image ID: {}", id);
        GalleryImageResponseDTO response = galleryImageFacade.refreshImageUrl(id);
        return ResponseEntity.ok(ApiResponse.success("Image URL refreshed successfully", response));
    }

    /**
     * Delete gallery image
     * Also deletes associated image from cloud storage
     * @param id Gallery image ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        log.info("Deleting gallery image ID: {}", id);
        galleryImageFacade.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }
}

