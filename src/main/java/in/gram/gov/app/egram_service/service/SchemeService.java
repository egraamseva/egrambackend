package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.enums.SchemeStatus;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Scheme;
import in.gram.gov.app.egram_service.domain.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeService {
    private final SchemeRepository schemeRepository;

    @Transactional
    public Scheme create(Scheme scheme) {
        log.info("SchemeService.create called - title={}", scheme.getTitle());
        return schemeRepository.save(scheme);
    }

    public Scheme findById(Long id) {
        log.debug("SchemeService.findById called - id={}", id);
        return schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", id));
    }

    public Page<Scheme> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("SchemeService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return schemeRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<Scheme> findByPanchayatIdAndStatus(Long panchayatId, SchemeStatus status, Pageable pageable) {
        return schemeRepository.findByPanchayatIdAndStatus(panchayatId, status, pageable);
    }

    public Page<Scheme> findActiveBySlug(String slug, Pageable pageable) {
        return schemeRepository.findActiveBySlug(slug, pageable);
    }

    @Transactional
    public Scheme update(Scheme scheme) {
        log.info("SchemeService.update called - id={}", scheme.getId());
        return schemeRepository.save(scheme);
    }

    @Transactional
    public void updateStatus(Long id, SchemeStatus status) {
        log.info("SchemeService.updateStatus called - id={}, status={}", id, status);
        Scheme scheme = findById(id);
        scheme.setStatus(status);
        schemeRepository.save(scheme);
    }

    @Transactional
    public void delete(Long id) {
        log.info("SchemeService.delete called - id={}", id);
        schemeRepository.deleteById(id);
    }
}
