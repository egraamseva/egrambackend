package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.domain.entity.AuditLog;
import in.gram.gov.app.egram_service.domain.repository.AuditLogRepository;
import in.gram.gov.app.egram_service.dto.filters.AuditFilter;
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
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog create(AuditLog auditLog) {
        log.info("AuditLogService.create called - actionType={}, panchayatId={}", auditLog.getActionType(), auditLog.getPanchayat()!=null?auditLog.getPanchayat().getId():null);
        return auditLogRepository.save(auditLog);
    }

    public Page<AuditLog> findByFilters(AuditFilter auditFilter) {
        log.info("AuditLogService.findByFilters called - filter={}", auditFilter);

        Pageable pageable = auditFilter.createPageable(auditFilter);
        Specification<AuditLog> auditSpecification = buildSpecification(auditFilter);

        return auditLogRepository.findAll(auditSpecification, pageable);
    }

    public Specification<AuditLog> buildSpecification(AuditFilter filter) {
        log.debug("AuditLogService.buildSpecification called - filter={}", filter);
        return SpecificationBuilder.<AuditLog>builder()
                .equalTo("panchayat.id", filter.getPanchayatId())
                .equalTo("actionType", filter.getActionType())
                .dateTimeRange("createdAt",
                        filter.getStartDate() != null ? filter.getStartDate() : null,
                        filter.getEndDate() != null ? filter.getEndDate() : null)
                .build();
    }


}
