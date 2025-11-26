package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.GalleryImage;
import in.gram.gov.app.egram_service.domain.repository.GalleryImageRepository;
import in.gram.gov.app.egram_service.dto.filters.GalleryFilter;
import in.gram.gov.app.egram_service.utility.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GalleryImageService {
    private final GalleryImageRepository galleryImageRepository;

    @Transactional
    public GalleryImage create(GalleryImage image) {
        log.info("GalleryImageService.create called - caption={}", image.getCaption());
        return galleryImageRepository.save(image);
    }

    public GalleryImage findById(Long id) {
        log.info("GalleryImageService.findById called - id={}", id);
        return galleryImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GalleryImage", id));
    }

    public Page<GalleryImage> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("GalleryImageService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return galleryImageRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<GalleryImage> findByPanchayatIdAndAlbumId(Long panchayatId, Long albumId, Pageable pageable) {
        log.info("GalleryImageService.findByPanchayatIdAndAlbumId called - panchayatId={}, albumId={}", panchayatId, albumId);
        return galleryImageRepository.findByPanchayatIdAndAlbumId(panchayatId, albumId, pageable);
    }

    public Page<GalleryImage> findByPanchayatSlugAndAlbumId(String slug, Long albumId, Pageable pageable) {
        log.info("GalleryImageService.findByPanchayatSlugAndAlbumId called - slug={}, albumId={}", slug, albumId);
        return galleryImageRepository.findByPanchayatSlugAndAlbumId(slug, albumId, pageable);
    }

    public Page<GalleryImage> findAll(GalleryFilter galleryFilter) {
        log.info("GalleryImageService.findAll called - filter={}", galleryFilter);
        Pageable pageable = galleryFilter.createPageable(galleryFilter);
        Specification<GalleryImage> postSpecification = buildSpecification(galleryFilter);
        return galleryImageRepository.findAll(postSpecification, pageable);
    }

    public Specification<GalleryImage> buildSpecification(GalleryFilter filter) {
        log.debug("GalleryImageService.buildSpecification called - filter={}", filter);
        return SpecificationBuilder.<GalleryImage>builder()
                .equalTo("album.id", filter.getAlbumId())
                .equalTo("panchayat.id", filter.getPanchayatId())
                .build();
    }

    @Transactional
    public GalleryImage update(GalleryImage image) {
        log.info("GalleryImageService.update called - id={}", image.getId());
        return galleryImageRepository.save(image);
    }

    @Transactional
    public void delete(Long id) {
        log.info("GalleryImageService.delete called - id={}", id);
        galleryImageRepository.deleteById(id);
    }
}
