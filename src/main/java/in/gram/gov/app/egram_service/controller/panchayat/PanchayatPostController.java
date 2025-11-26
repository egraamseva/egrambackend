package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.filters.PostFilter;
import in.gram.gov.app.egram_service.dto.request.PostRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PostResponseDTO;
import in.gram.gov.app.egram_service.facade.PostFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/panchayat/posts")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
public class PanchayatPostController {
    private final PostFacade postFacade;

    /**
     * Create a new post with optional image upload
     * Accepts both form data with image file and JSON payload
     * @param title Post title
     * @param bodyText Post body content
     * @param mediaUrl Optional media URL (if not uploading file)
     * @param imageFile Optional image file to upload and compress
     * @param compressionQuality Compression quality for image (HIGH, MEDIUM, LOW)
     * @param authentication Current user authentication
     * @return Created post with image URL if uploaded
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponseDTO>> create(
            @RequestParam(required = false) String title,
            @RequestParam String bodyText,
            @RequestParam(required = false) String mediaUrl,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            Authentication authentication) {

        String email = authentication.getName();

        // Build PostRequestDTO from form parameters
        PostRequestDTO request = new PostRequestDTO();
        request.setTitle(title);
        request.setBodyText(bodyText);
        request.setMediaUrl(mediaUrl);
        request.setImageFile(imageFile);

        try {
            request.setCompressionQuality(CompressionQuality.valueOf(compressionQuality.toUpperCase()));
        } catch (IllegalArgumentException e) {
            request.setCompressionQuality(CompressionQuality.HIGH);
        }

        log.info("Creating post for user: {}, with image upload: {}",
                email, imageFile != null && !imageFile.isEmpty());

        PostResponseDTO response = postFacade.create(request, email);
        return ResponseEntity.ok(ApiResponse.success("Post created successfully", response));
    }

    /**
     * Get all posts with optional filtering
     * @param postFilter Filter criteria
     * @return Paginated list of posts
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PostResponseDTO>>> getAll(
            PostFilter postFilter) {
        log.info("Fetching all posts with filter: {}", postFilter);
        PagedResponse<PostResponseDTO> response = PagedResponse.of(postFacade.getAll(postFilter));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get post by ID
     * @param id Post ID
     * @return Post details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> getById(@PathVariable Long id) {
        log.info("Fetching post by ID: {}", id);
        PostResponseDTO response = postFacade.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update existing post with optional new image
     * Accepts both form data with image file and JSON payload
     * @param id Post ID to update
     * @param title Updated title
     * @param bodyText Updated body
     * @param mediaUrl Optional media URL
     * @param imageFile Optional new image file to upload
     * @param compressionQuality Compression quality for new image
     * @return Updated post
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponseDTO>> update(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String bodyText,
            @RequestParam(required = false) String mediaUrl,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality) {

        // Build PostRequestDTO from form parameters
        PostRequestDTO request = new PostRequestDTO();
        request.setTitle(title);
        request.setBodyText(bodyText);
        request.setMediaUrl(mediaUrl);
        request.setImageFile(imageFile);

        try {
            request.setCompressionQuality(CompressionQuality.valueOf(compressionQuality.toUpperCase()));
        } catch (IllegalArgumentException e) {
            request.setCompressionQuality(CompressionQuality.HIGH);
        }

        log.info("Updating post ID: {}, with new image: {}",
                id, imageFile != null && !imageFile.isEmpty());

        PostResponseDTO response = postFacade.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", response));
    }

    /**
     * Publish a draft post
     * @param id Post ID to publish
     * @return Success message
     */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Object>> publish(@PathVariable Long id) {
        log.info("Publishing post ID: {}", id);
        postFacade.publish(id);
        return ResponseEntity.ok(ApiResponse.success("Post published successfully", null));
    }

    /**
     * Refresh presigned URL for post image when expired
     * Call this endpoint when the image URL returns 403 Forbidden
     * @param id Post ID
     * @return Updated post with fresh presigned URL
     */
    @PatchMapping("/{id}/refresh-image-url")
    public ResponseEntity<ApiResponse<PostResponseDTO>> refreshImageUrl(@PathVariable Long id) {
        log.info("Refreshing image URL for post ID: {}", id);
        PostResponseDTO response = postFacade.refreshImageUrl(id);
        return ResponseEntity.ok(ApiResponse.success("Image URL refreshed successfully", response));
    }

    /**
     * Delete a post
     * Also deletes associated image from cloud storage
     * @param id Post ID to delete
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        log.info("Deleting post ID: {}", id);
        postFacade.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }
}

