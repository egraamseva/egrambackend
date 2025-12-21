package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.SectionTemplate;
import in.gram.gov.app.egram_service.domain.repository.SectionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionTemplateService {
    private final SectionTemplateRepository repository;

    public List<SectionTemplate> findAllActive(String language) {
        log.info("SectionTemplateService.findAllActive called - language={}", language);
        if (language != null && !language.isEmpty()) {
            return repository.findByLanguageAndActive(language);
        }
        return repository.findByLanguageAndActive(null);
    }

    public List<SectionTemplate> findPageTemplates(String language) {
        log.info("SectionTemplateService.findPageTemplates called - language={}", language);
        return repository.findPageTemplatesByLanguage(language);
    }

    public List<SectionTemplate> findSectionTemplates(String language) {
        log.info("SectionTemplateService.findSectionTemplates called - language={}", language);
        return repository.findSectionTemplatesByLanguage(language);
    }

    public SectionTemplate findById(Long id) {
        log.debug("SectionTemplateService.findById called - id={}", id);
        return repository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("SectionTemplate", id));
    }

    @Transactional
    public SectionTemplate save(SectionTemplate template) {
        log.info("SectionTemplateService.save called - id={}", template.getId());
        return repository.save(template);
    }
}

