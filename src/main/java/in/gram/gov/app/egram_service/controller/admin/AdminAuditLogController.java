package in.gram.gov.app.egram_service.controller.admin;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.filters.AuditFilter;
import in.gram.gov.app.egram_service.dto.response.AuditLogResponseDTO;
import in.gram.gov.app.egram_service.facade.AdminFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('SUPER_ADMIN')")
@Slf4j
public class AdminAuditLogController {
    private final AdminFacade adminFacade;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponseDTO>>> getAuditLogs(
            AuditFilter auditFilter) {
        log.info("AdminAuditLogController.getAuditLogs called - filter={}", auditFilter);
        PagedResponse<AuditLogResponseDTO> response = PagedResponse.of(
                adminFacade.getAuditLogs(auditFilter));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
