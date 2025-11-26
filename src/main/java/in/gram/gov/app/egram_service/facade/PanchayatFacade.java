package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.PanchayatStatus;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.dto.filters.PanchayatFilter;
import in.gram.gov.app.egram_service.dto.request.PanchayatRequestDTO;
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
        panchayatService.delete(id);
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
}
