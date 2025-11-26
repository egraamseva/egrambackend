package in.gram.gov.app.egram_service.controller.open;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.filters.PanchayatFilter;
import in.gram.gov.app.egram_service.dto.response.*;
import in.gram.gov.app.egram_service.facade.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {
    private final PanchayatFacade panchayatFacade;
    private final PostFacade postFacade;
    private final SchemeFacade schemeFacade;
    private final AnnouncementFacade announcementFacade;
    private final GalleryImageFacade galleryImageFacade;
    private final UserFacade userFacade;

    @GetMapping("/panchayats")
    public ResponseEntity<ApiResponse<PagedResponse<PanchayatResponseDTO>>> getAllPanchayats(
            PanchayatFilter panchayatFilter) {
        log.info("PublicController.getAllPanchayats called - filter={}", panchayatFilter);
        PagedResponse<PanchayatResponseDTO> response = PagedResponse.of(
                panchayatFacade.getAll(panchayatFilter));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/panchayats/slug/{slug}")
    public ResponseEntity<ApiResponse<PanchayatResponseDTO>> getPanchayatBySlug(@PathVariable String slug) {
        log.info("PublicController.getPanchayatBySlug called - slug={}", slug);
        PanchayatResponseDTO response = panchayatFacade.getBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/home")
    public ResponseEntity<ApiResponse<PanchayatResponseDTO>> getHome(@PathVariable String slug) {
        log.info("PublicController.getHome called - slug={}", slug);
        PanchayatResponseDTO response = panchayatFacade.getBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/posts")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponseDTO>>> getPosts(
            @PathVariable String slug,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("PublicController.getPosts called - slug={}, pageable={}", slug, pageable);
        PagedResponse<PostResponseDTO> response = PagedResponse.of(postFacade.getPublishedBySlug(slug, pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponseDTO>> getPost(
            @PathVariable String slug,
            @PathVariable Long id) {
        log.info("PublicController.getPost called - slug={}, id={}", slug, id);
        PostResponseDTO response = postFacade.getPublishedByIdAndSlug(id, slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/schemes")
    public ResponseEntity<ApiResponse<PagedResponse<SchemeResponseDTO>>> getSchemes(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        log.info("PublicController.getSchemes called - slug={}, page={}, size={}", slug, page, size);
        Page<SchemeResponseDTO> schemes = schemeFacade.getActiveBySlug(slug, page, size);
        PagedResponse<SchemeResponseDTO> response = PagedResponse.of(schemes);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/announcements")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponseDTO>>> getAnnouncements(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        log.info("PublicController.getAnnouncements called - slug={}, page={}, size={}", slug, page, size);
        Page<AnnouncementResponseDTO> announcements = announcementFacade.getActiveBySlug(slug, page, size);
        PagedResponse<AnnouncementResponseDTO> response = PagedResponse.of(announcements);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/gallery")
    public ResponseEntity<ApiResponse<PagedResponse<GalleryImageResponseDTO>>> getGallery(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) Long albumId) {
        log.info("PublicController.getGallery called - slug={}, page={}, size={}, albumId={}", slug, page, size, albumId);
        Page<GalleryImageResponseDTO> images = galleryImageFacade.getBySlug(slug, page, size, albumId);
        PagedResponse<GalleryImageResponseDTO> response = PagedResponse.of(images);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/members")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDTO>>> getMembers(
            @PathVariable String slug,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("PublicController.getMembers called - slug={}, pageable={}", slug, pageable);
        PagedResponse<UserResponseDTO> response = PagedResponse.of(userFacade.getTeamMembersBySlug(slug, pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
