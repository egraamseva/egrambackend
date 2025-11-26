package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Announcement;
import in.gram.gov.app.egram_service.domain.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    @Transactional
    public Announcement create(Announcement announcement) {
        log.info("AnnouncementService.create called - announcement={}", announcement);
        return announcementRepository.save(announcement);
    }

    public Announcement findById(Long id) {
        log.info("AnnouncementService.findById called - id={}", id);
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }

    public Page<Announcement> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("AnnouncementService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return announcementRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<Announcement> findActiveBySlug(String slug, Pageable pageable) {
        log.info("AnnouncementService.findActiveBySlug called - slug={}, pageable={}", slug, pageable);
        return announcementRepository.findActiveBySlug(slug, LocalDate.now(), pageable);
    }

    @Transactional
    public Announcement update(Announcement announcement) {
        log.info("AnnouncementService.update called - announcement={}", announcement);
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void updateStatus(Long id, Boolean isActive) {
        log.info("AnnouncementService.updateStatus called - id={}, isActive={}", id, isActive);
        Announcement announcement = findById(id);
        announcement.setIsActive(isActive);
        announcementRepository.save(announcement);
    }

    @Transactional
    public void delete(Long id) {
        log.info("AnnouncementService.delete called - id={}", id);
        announcementRepository.deleteById(id);
    }
}
