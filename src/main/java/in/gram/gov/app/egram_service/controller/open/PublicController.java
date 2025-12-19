package in.gram.gov.app.egram_service.controller.open;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.filters.AlbumFilter;
import in.gram.gov.app.egram_service.dto.filters.PanchayatFilter;
import in.gram.gov.app.egram_service.dto.response.AlbumResponseDTO;
import in.gram.gov.app.egram_service.dto.response.AnnouncementResponseDTO;
import in.gram.gov.app.egram_service.dto.response.GalleryImageResponseDTO;
import in.gram.gov.app.egram_service.dto.response.NewsletterResponseDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatResponseDTO;
import in.gram.gov.app.egram_service.dto.response.PostResponseDTO;
import in.gram.gov.app.egram_service.dto.response.SchemeResponseDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteConfigDTO;
import in.gram.gov.app.egram_service.dto.response.PlatformLandingPageConfigDTO;
import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.dto.response.DocumentResponseDTO;
import in.gram.gov.app.egram_service.facade.AlbumFacadeNew;
import in.gram.gov.app.egram_service.facade.AnnouncementFacade;
import in.gram.gov.app.egram_service.facade.DocumentFacade;
import in.gram.gov.app.egram_service.facade.GalleryImageFacade;
import in.gram.gov.app.egram_service.facade.NewsletterFacade;
import in.gram.gov.app.egram_service.facade.PanchayatFacade;
import in.gram.gov.app.egram_service.facade.PanchayatWebsiteFacade;
import in.gram.gov.app.egram_service.facade.PlatformLandingPageFacade;
import in.gram.gov.app.egram_service.facade.PostFacade;
import in.gram.gov.app.egram_service.facade.SchemeFacade;
import in.gram.gov.app.egram_service.facade.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final NewsletterFacade newsletterFacade;
    private final AlbumFacadeNew albumFacade;
    private final PlatformLandingPageFacade platformLandingPageFacade;
    private final PanchayatWebsiteFacade panchayatWebsiteFacade;
    private final DocumentFacade documentFacade;


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

    @GetMapping("/{slug}/newsletters")
    public ResponseEntity<ApiResponse<PagedResponse<NewsletterResponseDTO>>> getNewsletters(
            @PathVariable String slug,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("PublicController.getNewsletters called - slug={}, search={}, pageable={}", slug, search, pageable);
        PagedResponse<NewsletterResponseDTO> response = PagedResponse.of(
                newsletterFacade.getPublishedBySlug(slug, search, pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/newsletters/{id}")
    public ResponseEntity<ApiResponse<NewsletterResponseDTO>> getNewsletter(
            @PathVariable String slug,
            @PathVariable Long id) {
        log.info("PublicController.getNewsletter called - slug={}, id={}", slug, id);
        NewsletterResponseDTO response = newsletterFacade.getPublishedByIdAndSlug(id, slug);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("{slug}/albums")
    public ResponseEntity<ApiResponse<PagedResponse<AlbumResponseDTO>>> getAll(
            @PathVariable String slug, @ModelAttribute AlbumFilter albumFilter) {
        albumFilter.setSortBy("createdAt");
        albumFilter.setSortOrder(Sort.Direction.DESC);
        albumFilter.setPanchayatSlug(slug);
        Page<AlbumResponseDTO> albums = albumFacade.getAll(albumFilter);
        PagedResponse<AlbumResponseDTO> response = PagedResponse.of(albums);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/platform/landing-page")
    public ResponseEntity<ApiResponse<PlatformLandingPageConfigDTO>> getPlatformLandingPage() {
        log.info("PublicController.getPlatformLandingPage called");
        List<in.gram.gov.app.egram_service.dto.response.PlatformSectionResponseDTO> sections = platformLandingPageFacade.getVisibleSections();
        PlatformLandingPageConfigDTO config = new PlatformLandingPageConfigDTO();
        config.setSections(sections);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @GetMapping("/{slug}/website")
    public ResponseEntity<ApiResponse<PanchayatWebsiteConfigDTO>> getPanchayatWebsite(@PathVariable String slug) {
        log.info("PublicController.getPanchayatWebsite called - slug={}", slug);
        List<in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteSectionResponseDTO> sections = panchayatWebsiteFacade.getVisibleSections(slug);
        PanchayatWebsiteConfigDTO config = new PanchayatWebsiteConfigDTO();
        config.setSections(sections);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @GetMapping("/{slug}/documents")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDTO>>> getPublicDocuments(
            @PathVariable String slug,
            @RequestParam(required = false) DocumentCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("PublicController.getPublicDocuments called - slug={}, category={}", slug, category);
        PagedResponse<DocumentResponseDTO> response = documentFacade.getPublicDocuments(slug, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{slug}/documents/{id}/view")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> getPublicDocumentView(
            @PathVariable String slug,
            @PathVariable Long id) {
        log.info("PublicController.getPublicDocumentView called - slug={}, id={}", slug, id);
        // This will be handled by the facade - it should only return public documents
        // For now, we'll use the same endpoint but ensure it's public
        PagedResponse<DocumentResponseDTO> documents = documentFacade.getPublicDocuments(slug, null, 
                org.springframework.data.domain.PageRequest.of(0, 1000));
        
        DocumentResponseDTO document = documents.getContent().stream()
                .filter(doc -> doc.getDocumentId().equals(id))
                .findFirst()
                .orElse(null);
        
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @GetMapping("/{slug}/website-documents")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDTO>>> getWebsiteDocuments(
            @PathVariable String slug,
            @RequestParam(required = false) DocumentCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("PublicController.getWebsiteDocuments called - slug={}, category={}", slug, category);
        PagedResponse<DocumentResponseDTO> response = documentFacade.getWebsiteDocuments(slug, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
