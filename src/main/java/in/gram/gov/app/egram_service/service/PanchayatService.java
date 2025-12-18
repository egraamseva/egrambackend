package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.enums.PanchayatStatus;
import in.gram.gov.app.egram_service.constants.exception.DuplicateResourceException;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.repository.PanchayatRepository;
import in.gram.gov.app.egram_service.dto.filters.PanchayatFilter;
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
public class PanchayatService {
    private final PanchayatRepository panchayatRepository;

    @Transactional
    public Panchayat create(Panchayat panchayat) {
        log.info("PanchayatService.create called - slug={}", panchayat.getSlug());
        if (panchayatRepository.existsBySlug(panchayat.getSlug())) {
            throw new DuplicateResourceException("Panchayat with slug " + panchayat.getSlug() + " already exists");
        }
        return panchayatRepository.save(panchayat);
    }

    public Panchayat findById(Long id) {
        log.debug("PanchayatService.findById called - id={}", id);
        return panchayatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panchayat", id));
    }

    public Panchayat findBySlug(String slug) {
        log.debug("PanchayatService.findBySlug called - slug={}", slug);
        return panchayatRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Panchayat with slug " + slug + " not found"));
    }

    public Page<Panchayat> findAll(Pageable pageable) {
        log.info("PanchayatService.findAll called - pageable={}", pageable);
        return panchayatRepository.findAll(pageable);
    }

    public Page<Panchayat> findByStatus(PanchayatStatus status, Pageable pageable) {
        log.info("PanchayatService.findByStatus called - status={}, pageable={}", status, pageable);
        return panchayatRepository.findByStatus(status, pageable);
    }


    public Specification<Panchayat> buildSpecification(PanchayatFilter filter) {
        log.debug("PanchayatService.buildSpecification called - filter={}", filter);
        Specification<Panchayat> spec = Specification.where(null);
        
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        
        if (filter.getDistrict() != null && !filter.getDistrict().trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("district")), "%" + filter.getDistrict().toLowerCase() + "%"));
        }
        
        if (filter.getState() != null && !filter.getState().trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("state")), "%" + filter.getState().toLowerCase() + "%"));
        }
        
        // Search query - search in name, slug, district, or state
        if (filter.getSearchQuery() != null && !filter.getSearchQuery().trim().isEmpty()) {
            String searchTerm = filter.getSearchQuery().toLowerCase().trim();
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("panchayatName")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("slug")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("district")), "%" + searchTerm + "%"),
                    cb.like(cb.lower(root.get("state")), "%" + searchTerm + "%")
                )
            );
        }
        
        return spec;
    }

    public Page<Panchayat> findByFilters(PanchayatFilter panchayatFilter) {
        log.info("PanchayatService.findByFilters called - filter={}", panchayatFilter);
        Pageable pageable = panchayatFilter.createPageable(panchayatFilter);
        Specification<Panchayat> panchayatSpecification = buildSpecification(panchayatFilter);
        return panchayatRepository.findAll(panchayatSpecification, pageable);
    }

    @Transactional
    public Panchayat update(Panchayat panchayat) {
        log.info("PanchayatService.update called - id={}", panchayat.getId());
        return panchayatRepository.save(panchayat);
    }

    @Transactional
    public void updateStatus(Long id, PanchayatStatus status) {
        log.info("PanchayatService.updateStatus called - id={}, status={}", id, status);
        Panchayat panchayat = findById(id);
        panchayat.setStatus(status);
        panchayatRepository.save(panchayat);
    }

    @Transactional
    public void delete(Long id) {
        log.info("PanchayatService.delete called - id={}", id);
        Panchayat panchayat = findById(id);
        panchayat.setStatus(PanchayatStatus.INACTIVE);
        panchayatRepository.save(panchayat);
        log.info("Panchayat {} marked as INACTIVE", id);
    }
}
