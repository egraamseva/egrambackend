package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.request.AlbumRequestDTO;
import in.gram.gov.app.egram_service.dto.response.AlbumResponseDTO;
import in.gram.gov.app.egram_service.facade.AlbumFacadeNew;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Panchayat Album Controller
 * Manages album operations including creation with cover image upload, retrieval, update, and deletion
 * Supports both direct file upload with compression and URL-only workflows
 */
@RestController
@RequestMapping("/api/v1/panchayat/albums")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
@Slf4j
public class PanchayatAlbumController {
    private final AlbumFacadeNew albumFacade;

    /**
     * Create album with optional cover image upload
     * Accepts both form data with cover image file and JSON payload for URL-only workflow
     * @param albumName Album name (required)
     * @param description Album description
     * @param coverImageFile Optional cover image file to upload and compress
     * @param compressionQuality Compression quality for cover image (HIGH, MEDIUM, LOW)
     * @param coverImageUrl Optional cover image URL (if not uploading file)
     * @return Created album with cover image URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AlbumResponseDTO>> create(
            @RequestParam String albumName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile coverImageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String coverImageUrl) {

        // Build AlbumRequestDTO from form parameters
        AlbumRequestDTO request = new AlbumRequestDTO();
        request.setAlbumName(albumName);
        request.setDescription(description);
        request.setCoverImageFile(coverImageFile);
        request.setCoverImageUrl(coverImageUrl);

        try {
            request.setCompressionQuality(compressionQuality.toUpperCase());
        } catch (Exception e) {
            request.setCompressionQuality("HIGH");
        }

        log.info("Creating album: {}, with cover image upload: {}",
                albumName, coverImageFile != null && !coverImageFile.isEmpty());

        AlbumResponseDTO response = albumFacade.create(request);
        return ResponseEntity.ok(ApiResponse.success("Album created successfully", response));
    }

    /**
     * Get all albums
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of albums
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AlbumResponseDTO>>> getAll(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        Page<AlbumResponseDTO> albums = albumFacade.getAll(page, size);
        PagedResponse<AlbumResponseDTO> response = PagedResponse.of(albums);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get album by ID
     * @param id Album ID
     * @return Album details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlbumResponseDTO>> getById(@PathVariable Long id) {
        AlbumResponseDTO response = albumFacade.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update album with optional new cover image
     * Accepts both form data with image file and JSON payload
     * If new cover image provided: compress, upload to B2, delete old image, and update URL
     * @param id Album ID
     * @param albumName Updated album name
     * @param description Updated description
     * @param coverImageFile Optional new cover image file to upload
     * @param compressionQuality Compression quality for new cover image
     * @param coverImageUrl Optional new cover image URL
     * @return Updated album
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AlbumResponseDTO>> update(
            @PathVariable Long id,
            @RequestParam(required = false) String albumName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile coverImageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String coverImageUrl) {

        // Build AlbumRequestDTO from form parameters
        AlbumRequestDTO request = new AlbumRequestDTO();
        request.setAlbumName(albumName);
        request.setDescription(description);
        request.setCoverImageFile(coverImageFile);
        request.setCoverImageUrl(coverImageUrl);

        try {
            request.setCompressionQuality(compressionQuality.toUpperCase());
        } catch (Exception e) {
            request.setCompressionQuality("HIGH");
        }

        log.info("Updating album ID: {}, with new cover image: {}",
                id, coverImageFile != null && !coverImageFile.isEmpty());

        AlbumResponseDTO response = albumFacade.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Album updated successfully", response));
    }

    /**
     * Refresh presigned URL for album cover image when expired
     * Call this endpoint when the cover image URL returns 403 Forbidden
     * @param id Album ID
     * @return Updated album with fresh presigned URL
     */
    @PatchMapping("/{id}/refresh-cover-image-url")
    public ResponseEntity<ApiResponse<AlbumResponseDTO>> refreshCoverImageUrl(@PathVariable Long id) {
        log.info("Refreshing cover image URL for album ID: {}", id);
        AlbumResponseDTO response = albumFacade.refreshCoverImageUrl(id);
        return ResponseEntity.ok(ApiResponse.success("Cover image URL refreshed successfully", response));
    }

    /**
     * Add multiple gallery images to an album
     * Associates existing gallery images with the specified album
     * @param id Album ID
     * @param payload JSON payload containing array of gallery image IDs
     *               Example: {"galleryImageIds": [1, 2, 3]}
     * @return Success message with count of images added
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addImagesToAlbum(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> payload) {

        List<Long> galleryImageIds = payload.get("galleryImageIds");

        if (galleryImageIds == null || galleryImageIds.isEmpty()) {
            log.warn("No gallery image IDs provided for album ID: {}", id);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Gallery image IDs are required")
            );
        }

        log.info("Adding {} gallery images to album ID: {}", galleryImageIds.size(), id);

        int addedCount = albumFacade.addImagesToAlbum(id, galleryImageIds);

        Map<String, Object> result = Map.of(
                "albumId", id,
                "imagesAdded", addedCount,
                "totalRequested", galleryImageIds.size()
        );

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully added %d images to album", addedCount),
                result
        ));
    }

    /**
     * Delete album
     * Also deletes associated cover image from cloud storage
     * Gallery images in the album are preserved but album association is cleared
     * @param id Album ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        log.info("Deleting album ID: {}", id);
        albumFacade.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Album deleted successfully", null));
    }
}
