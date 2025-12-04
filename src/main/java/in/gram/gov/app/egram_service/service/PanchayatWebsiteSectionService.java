package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.PanchayatWebsiteSection;
import in.gram.gov.app.egram_service.domain.repository.PanchayatWebsiteSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PanchayatWebsiteSectionService {
    private final PanchayatWebsiteSectionRepository repository;
    private final PanchayatService panchayatService;

    @Transactional
    public PanchayatWebsiteSection create(PanchayatWebsiteSection section) {
        log.info("PanchayatWebsiteSectionService.create called");
        // If displayOrder is not set, set it to the max + 1 for this panchayat
        if (section.getDisplayOrder() == null) {
            List<PanchayatWebsiteSection> allSections = repository.findByPanchayatIdOrdered(section.getPanchayat().getId());
            int maxOrder = allSections.stream()
                    .mapToInt(PanchayatWebsiteSection::getDisplayOrder)
                    .max()
                    .orElse(-1);
            section.setDisplayOrder(maxOrder + 1);
        }
        return repository.save(section);
    }

    public PanchayatWebsiteSection findById(Long id) {
        log.debug("PanchayatWebsiteSectionService.findById called - id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PanchayatWebsiteSection", id));
    }

    public List<PanchayatWebsiteSection> findByPanchayatIdAndVisible(Long panchayatId) {
        log.info("PanchayatWebsiteSectionService.findByPanchayatIdAndVisible called - panchayatId={}", panchayatId);
        return repository.findByPanchayatIdAndVisibleOrdered(panchayatId);
    }

    public List<PanchayatWebsiteSection> findByPanchayatId(Long panchayatId) {
        log.info("PanchayatWebsiteSectionService.findByPanchayatId called - panchayatId={}", panchayatId);
        return repository.findByPanchayatIdOrdered(panchayatId);
    }

    public List<PanchayatWebsiteSection> findByPanchayatSlugAndVisible(String slug) {
        log.info("PanchayatWebsiteSectionService.findByPanchayatSlugAndVisible called - slug={}", slug);
        return repository.findByPanchayatSlugAndVisibleOrdered(slug);
    }

    @Transactional
    public PanchayatWebsiteSection update(PanchayatWebsiteSection section) {
        log.info("PanchayatWebsiteSectionService.update called - id={}", section.getId());
        return repository.save(section);
    }

    @Transactional
    public void delete(Long id) {
        log.info("PanchayatWebsiteSectionService.delete called - id={}", id);
        repository.deleteById(id);
    }

    @Transactional
    public void updateDisplayOrder(Long id, Integer newOrder) {
        log.info("PanchayatWebsiteSectionService.updateDisplayOrder called - id={}, newOrder={}", id, newOrder);
        PanchayatWebsiteSection section = findById(id);
        section.setDisplayOrder(newOrder);
        repository.save(section);
    }

    @Transactional
    public void updateVisibility(Long id, Boolean isVisible) {
        log.info("PanchayatWebsiteSectionService.updateVisibility called - id={}, isVisible={}", id, isVisible);
        PanchayatWebsiteSection section = findById(id);
        section.setIsVisible(isVisible);
        repository.save(section);
    }
}

