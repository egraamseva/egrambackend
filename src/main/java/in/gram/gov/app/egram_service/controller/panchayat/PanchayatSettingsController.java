package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.request.PanchayatRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatResponseDTO;
import in.gram.gov.app.egram_service.facade.PanchayatFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/panchayat/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
@Slf4j
public class PanchayatSettingsController {
    private final PanchayatFacade panchayatFacade;

    @GetMapping
    public ResponseEntity<ApiResponse<PanchayatResponseDTO>> getSettings() {
        log.info("PanchayatSettingsController.getSettings called");
        PanchayatResponseDTO response = panchayatFacade.getCurrentPanchayat();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PanchayatResponseDTO>> updateSettings(
            @Valid @RequestBody PanchayatRequestDTO request) {
        log.info("PanchayatSettingsController.updateSettings called - request={}", request);
        PanchayatResponseDTO response = panchayatFacade.updateCurrent(request);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", response));
    }

    @PostMapping(value = "/hero-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PanchayatResponseDTO>> uploadHeroImage(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality) {
        log.info("PanchayatSettingsController.uploadHeroImage called - file={}, compressionQuality={}",
                imageFile.getOriginalFilename(), compressionQuality);
        
        CompressionQuality quality;
        try {
            quality = CompressionQuality.valueOf(compressionQuality.toUpperCase());
        } catch (Exception e) {
            quality = CompressionQuality.HIGH;
        }
        
        PanchayatResponseDTO response = panchayatFacade.uploadHeroImage(imageFile, quality);
        return ResponseEntity.ok(ApiResponse.success("Hero image uploaded successfully", response));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PanchayatResponseDTO>> uploadLogo(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality) {
        log.info("PanchayatSettingsController.uploadLogo called - file={}, compressionQuality={}",
                imageFile.getOriginalFilename(), compressionQuality);
        
        CompressionQuality quality;
        try {
            quality = CompressionQuality.valueOf(compressionQuality.toUpperCase());
        } catch (Exception e) {
            quality = CompressionQuality.HIGH;
        }
        
        PanchayatResponseDTO response = panchayatFacade.uploadLogo(imageFile, quality);
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded successfully", response));
    }
}
