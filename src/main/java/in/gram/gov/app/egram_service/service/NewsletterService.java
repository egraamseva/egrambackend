package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Newsletter;
import in.gram.gov.app.egram_service.domain.repository.NewsletterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {
    private final NewsletterRepository newsletterRepository;

    @Transactional
    public Newsletter create(Newsletter newsletter) {
        log.info("NewsletterService.create called - title={}", newsletter.getTitle());
        return newsletterRepository.save(newsletter);
    }

    public Newsletter findById(Long id) {
        log.debug("NewsletterService.findById called - id={}", id);
        return newsletterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Newsletter", id));
    }

    public Newsletter findPublishedByIdAndSlug(Long newsletterId, String slug) {
        log.debug("NewsletterService.findPublishedByIdAndSlug called - newsletterId={}, slug={}", newsletterId, slug);
        Newsletter newsletter = newsletterRepository.findById(newsletterId)
                .orElseThrow(() -> new ResourceNotFoundException("Newsletter", newsletterId));
        
        if (!newsletter.getIsPublished() || !newsletter.getPanchayat().getSlug().equals(slug)) {
            throw new ResourceNotFoundException("Newsletter", newsletterId);
        }
        
        return newsletter;
    }

    public Page<Newsletter> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("NewsletterService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return newsletterRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<Newsletter> findByPanchayatIdAndIsPublished(Long panchayatId, Boolean isPublished, Pageable pageable) {
        log.info("NewsletterService.findByPanchayatIdAndIsPublished called - panchayatId={}, isPublished={}", panchayatId, isPublished);
        return newsletterRepository.findByPanchayatIdAndIsPublished(panchayatId, isPublished, pageable);
    }

    public Page<Newsletter> findPublishedBySlug(String slug, Pageable pageable) {
        log.info("NewsletterService.findPublishedBySlug called - slug={}, pageable={}", slug, pageable);
        return newsletterRepository.findByPanchayatSlugAndPublished(slug, pageable);
    }

    public Page<Newsletter> findPublishedBySlugWithSearch(String slug, String search, Pageable pageable) {
        log.info("NewsletterService.findPublishedBySlugWithSearch called - slug={}, search={}, pageable={}", slug, search, pageable);
        if (search == null || search.trim().isEmpty()) {
            return findPublishedBySlug(slug, pageable);
        }
        return newsletterRepository.findByPanchayatSlugAndPublishedWithSearch(slug, search.trim(), pageable);
    }

    @Transactional
    public Newsletter update(Newsletter newsletter) {
        log.info("NewsletterService.update called - id={}", newsletter.getId());
        return newsletterRepository.save(newsletter);
    }

    @Transactional
    public void togglePublish(Long id) {
        log.info("NewsletterService.togglePublish called - id={}", id);
        Newsletter newsletter = findById(id);
        newsletter.setIsPublished(!newsletter.getIsPublished());
        if (newsletter.getIsPublished() && newsletter.getPublishedOn() == null) {
            newsletter.setPublishedOn(java.time.LocalDate.now());
        }
        newsletterRepository.save(newsletter);
    }

    @Transactional
    public void delete(Long id) {
        log.info("NewsletterService.delete called - id={}", id);
        newsletterRepository.deleteById(id);
    }
}

