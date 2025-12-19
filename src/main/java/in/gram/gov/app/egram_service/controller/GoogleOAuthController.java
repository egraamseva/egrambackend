package in.gram.gov.app.egram_service.controller;

import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.service.GoogleDriveService;
import in.gram.gov.app.egram_service.service.PanchayatService;
import in.gram.gov.app.egram_service.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthController {
    private final GoogleDriveService googleDriveService;
    private final UserService userService;
    private final PanchayatService panchayatService;
    
    @Value("${google.oauth.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/authorize")
    public ResponseEntity<ApiResponse<Map<String, String>>> authorize(
            Authentication authentication) throws IOException {
        log.info("GoogleOAuthController.authorize called");
        
        // Get panchayat from tenant context (panchayat email should be used for documents)
        Long panchayatId = TenantContext.getTenantId();
        if (panchayatId == null) {
            throw new IllegalStateException("Panchayat context not available");
        }
        
        var panchayat = panchayatService.findById(panchayatId);
        String panchayatEmail = panchayat.getOfficeEmail() != null && !panchayat.getOfficeEmail().isEmpty() 
                ? panchayat.getOfficeEmail() 
                : panchayat.getContactEmail();
        
        if (panchayatEmail == null || panchayatEmail.isEmpty()) {
            throw new IllegalStateException("Panchayat email not configured. Please set office email or contact email.");
        }
        
        log.info("Using panchayat email for Google OAuth: {}", panchayatEmail);
        
        String authorizationUrl = googleDriveService.getAuthorizationUrl(panchayatId);
        
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authorizationUrl);
        
        return ResponseEntity.ok(ApiResponse.success("Authorization URL generated", response));
    }

    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) throws IOException {
        log.info("GoogleOAuthController.handleCallback called - state={}", state);
        
        try {
            googleDriveService.handleOAuthCallback(code, state);
            
            // Redirect to frontend success page (use full frontend URL)
            String redirectUrl = frontendUrl + "/panchayat/dashboard/documents?google_connected=true";
            log.info("Redirecting to frontend: {}", redirectUrl);
            httpResponse.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Error handling OAuth callback", e);
            String redirectUrl = frontendUrl + "/panchayat/dashboard/documents?google_error=true";
            log.info("Redirecting to frontend with error: {}", redirectUrl);
            httpResponse.sendRedirect(redirectUrl);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionStatus(Authentication authentication) {
        log.info("GoogleOAuthController.getConnectionStatus called");
        
        // Get panchayat from tenant context
        Long panchayatId = TenantContext.getTenantId();
        if (panchayatId == null) {
            throw new IllegalStateException("Panchayat context not available");
        }
        
        boolean isConnected = googleDriveService.isConnected(panchayatId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isConnected", isConnected);
        
        return ResponseEntity.ok(ApiResponse.success("Connection status retrieved", response));
    }

    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<Object>> revokeAccess(Authentication authentication) {
        log.info("GoogleOAuthController.revokeAccess called");
        
        // Get panchayat from tenant context
        Long panchayatId = TenantContext.getTenantId();
        if (panchayatId == null) {
            throw new IllegalStateException("Panchayat context not available");
        }
        
        googleDriveService.revokeAccess(panchayatId);

        return ResponseEntity.ok(ApiResponse.success("Google Drive access revoked successfully", null));
    }
}

