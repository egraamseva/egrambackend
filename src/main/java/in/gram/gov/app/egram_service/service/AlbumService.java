package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Album;
import in.gram.gov.app.egram_service.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlbumService {
    private final AlbumRepository albumRepository;

    @Transactional
    public Album create(Album album) {
        log.info("AlbumService.create called - album={}", album);
        return albumRepository.save(album);
    }

    public Album findById(Long id) {
        log.info("AlbumService.findById called - id={}", id);
        return albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album", id));
    }

    public Page<Album> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("AlbumService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return albumRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<Album> findByPanchayatSlug(String slug, Pageable pageable) {
        log.info("AlbumService.findByPanchayatSlug called - slug={}, pageable={}", slug, pageable);
        return albumRepository.findByPanchayatSlug(slug, pageable);
    }

    @Transactional
    public Album update(Album album) {
        log.info("AlbumService.update called - album={}", album);
        return albumRepository.save(album);
    }

    @Transactional
    public void delete(Long id) {
        log.info("AlbumService.delete called - id={}", id);
        albumRepository.deleteById(id);
    }
}
