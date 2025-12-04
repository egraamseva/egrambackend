package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.PlatformLandingPageSection;
import in.gram.gov.app.egram_service.domain.repository.PlatformLandingPageSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformLandingPageSectionService {
    private final PlatformLandingPageSectionRepository repository;

    @Transactional
    public PlatformLandingPageSection create(PlatformLandingPageSection section) {
        log.info("PlatformLandingPageSectionService.create called");
        // If displayOrder is not set, set it to the max + 1
        if (section.getDisplayOrder() == null) {
            List<PlatformLandingPageSection> allSections = repository.findAllOrdered();
            int maxOrder = allSections.stream()
                    .mapToInt(PlatformLandingPageSection::getDisplayOrder)
                    .max()
                    .orElse(-1);
            section.setDisplayOrder(maxOrder + 1);
        }
        return repository.save(section);
    }

    public PlatformLandingPageSection findById(Long id) {
        log.debug("PlatformLandingPageSectionService.findById called - id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformLandingPageSection", id));
    }

    public List<PlatformLandingPageSection> findAllVisible() {
        log.info("PlatformLandingPageSectionService.findAllVisible called");
        return repository.findAllVisibleOrdered();
    }

    public List<PlatformLandingPageSection> findAll() {
        log.info("PlatformLandingPageSectionService.findAll called");
        return repository.findAllOrdered();
    }

    @Transactional
    public PlatformLandingPageSection update(PlatformLandingPageSection section) {
        log.info("PlatformLandingPageSectionService.update called - id={}", section.getId());
        return repository.save(section);
    }

    @Transactional
    public void delete(Long id) {
        log.info("PlatformLandingPageSectionService.delete called - id={}", id);
        repository.deleteById(id);
    }

    @Transactional
    public void updateDisplayOrder(Long id, Integer newOrder) {
        log.info("PlatformLandingPageSectionService.updateDisplayOrder called - id={}, newOrder={}", id, newOrder);
        PlatformLandingPageSection section = findById(id);
        section.setDisplayOrder(newOrder);
        repository.save(section);
    }

    @Transactional
    public void updateVisibility(Long id, Boolean isVisible) {
        log.info("PlatformLandingPageSectionService.updateVisibility called - id={}, isVisible={}", id, isVisible);
        PlatformLandingPageSection section = findById(id);
        section.setIsVisible(isVisible);
        repository.save(section);
    }
}

