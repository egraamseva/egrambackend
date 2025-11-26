package in.gram.gov.app.egram_service.controller.admin;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.response.SystemAnalyticsResponseDTO;
import in.gram.gov.app.egram_service.facade.AdminFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('SUPER_ADMIN')")
@Slf4j
public class AdminAnalyticsController {
    private final AdminFacade adminFacade;

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<SystemAnalyticsResponseDTO>> getSystemAnalytics() {
        log.info("AdminAnalyticsController.getSystemAnalytics called");
        SystemAnalyticsResponseDTO response = adminFacade.getSystemAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
