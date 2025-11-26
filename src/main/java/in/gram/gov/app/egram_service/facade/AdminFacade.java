package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.PanchayatStatus;
import in.gram.gov.app.egram_service.constants.enums.UserRole;
import in.gram.gov.app.egram_service.constants.enums.UserStatus;
import in.gram.gov.app.egram_service.domain.entity.AuditLog;
import in.gram.gov.app.egram_service.domain.repository.*;
import in.gram.gov.app.egram_service.dto.filters.AuditFilter;
import in.gram.gov.app.egram_service.dto.response.AuditLogResponseDTO;
import in.gram.gov.app.egram_service.dto.response.SystemAnalyticsResponseDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;
import in.gram.gov.app.egram_service.service.AuditLogService;
import in.gram.gov.app.egram_service.service.PanchayatService;
import in.gram.gov.app.egram_service.service.UserService;
import in.gram.gov.app.egram_service.transformer.AuditLogTransformer;
import in.gram.gov.app.egram_service.transformer.UserTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {
    private final UserService userService;
    private final PanchayatService panchayatService;
    private final PostRepository postRepository;
    private final AnnouncementRepository announcementRepository;
    private final SchemeRepository schemeRepository;
    private final DocumentRepository documentRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final AuditLogService auditLogService;

    public Page<UserResponseDTO> getAllUsers(UserRole role, UserStatus status, Pageable pageable) {
        log.info("AdminFacade.getAllUsers called - role={}, status={}, pageable={}", role, status, pageable);
        // Get all users (panchayatId = null means all panchayats)
        Page<in.gram.gov.app.egram_service.domain.entity.User> users = 
            userService.findByFilters(null, role, status, pageable);
        return users.map(UserTransformer::toDTO);
    }

    public SystemAnalyticsResponseDTO getSystemAnalytics() {
        log.info("AdminFacade.getSystemAnalytics called");
        SystemAnalyticsResponseDTO analytics = new SystemAnalyticsResponseDTO();
        
        // Count all panchayats
        analytics.setTotalPanchayats(panchayatService.findAll(Pageable.unpaged()).getTotalElements());
        
        // Count active panchayats
        analytics.setActivePanchayats(
            panchayatService.findByStatus(PanchayatStatus.ACTIVE, Pageable.unpaged()).getTotalElements()
        );
        
        // Count all users
        analytics.setTotalUsers(userService.findByFilters(null, null, null, Pageable.unpaged()).getTotalElements());
        
        // Count all posts
        analytics.setTotalPosts(postRepository.findAll(Pageable.unpaged()).getTotalElements());
        
        // Count all schemes
        analytics.setTotalSchemes(schemeRepository.findAll(Pageable.unpaged()).getTotalElements());
        
        // Count all announcements
        analytics.setTotalAnnouncements(announcementRepository.findAll(Pageable.unpaged()).getTotalElements());
        
        // Count all documents
        analytics.setTotalDocuments(documentRepository.findAll(Pageable.unpaged()).getTotalElements());
        
        // Count all gallery images
        analytics.setTotalGalleryImages(galleryImageRepository.findAll(Pageable.unpaged()).getTotalElements());
        
        return analytics;
    }

    public Page<AuditLogResponseDTO> getAuditLogs(AuditFilter auditFilter) {
        log.info("AdminFacade.getAuditLogs called - filter={}", auditFilter);
        Page<AuditLog> logs = auditLogService.findByFilters(auditFilter);
        return logs.map(AuditLogTransformer::toDTO);
    }
}
