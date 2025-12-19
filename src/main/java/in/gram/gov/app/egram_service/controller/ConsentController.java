package in.gram.gov.app.egram_service.controller;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.service.ConsentService;
import in.gram.gov.app.egram_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
@Slf4j
public class ConsentController {
    private final ConsentService consentService;
    private final UserService userService;

    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<Map<String, Object>>> grantConsent(
            Authentication authentication,
            HttpServletRequest request) {
        log.info("ConsentController.grantConsent called");
        
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        String ipAddress = getClientIpAddress(request);

        var consent = consentService.recordConsent(user.getId(), ipAddress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("consentId", consent.getId());
        response.put("consentTimestamp", consent.getConsentTimestamp());
        response.put("purpose", consent.getPurpose());

        return ResponseEntity.ok(ApiResponse.success("Consent granted successfully", response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConsentStatus(Authentication authentication) {
        log.info("ConsentController.getConsentStatus called");
        
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        
        boolean hasConsent = consentService.hasValidConsent(user.getId());
        var activeConsent = consentService.getActiveConsent(user.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasConsent", hasConsent);
        if (activeConsent != null) {
            response.put("consentTimestamp", activeConsent.getConsentTimestamp());
            response.put("purpose", activeConsent.getPurpose());
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<Object>> revokeConsent(
            Authentication authentication,
            @RequestParam(required = false) String reason) {
        log.info("ConsentController.revokeConsent called");
        
        String email = authentication.getName();
        var user = userService.findByEmail(email);
        
        consentService.revokeConsent(user.getId(), reason != null ? reason : "User requested revocation");

        return ResponseEntity.ok(ApiResponse.success("Consent revoked successfully", null));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

