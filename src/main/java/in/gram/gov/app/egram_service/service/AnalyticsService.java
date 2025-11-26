package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.domain.entity.Analytics;
import in.gram.gov.app.egram_service.domain.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    @Transactional
    public Analytics create(Analytics analytics) {
        log.info("AnalyticsService.create called - analytics={}", analytics);
        return analyticsRepository.save(analytics);
    }

    public Page<Analytics> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("AnalyticsService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return analyticsRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Long countByPanchayatIdAndDateRange(Long panchayatId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("AnalyticsService.countByPanchayatIdAndDateRange called - panchayatId={}, startDate={}, endDate={}", panchayatId, startDate, endDate);
        return analyticsRepository.countByPanchayatIdAndDateRange(panchayatId, startDate, endDate);
    }

    public Long countUniqueSessionsByPanchayatIdAndDateRange(Long panchayatId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("AnalyticsService.countUniqueSessionsByPanchayatIdAndDateRange called - panchayatId={}, startDate={}, endDate={}", panchayatId, startDate, endDate);
        return analyticsRepository.countUniqueSessionsByPanchayatIdAndDateRange(panchayatId, startDate, endDate);
    }
}
