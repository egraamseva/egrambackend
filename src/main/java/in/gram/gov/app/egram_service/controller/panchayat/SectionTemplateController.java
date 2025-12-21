package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.request.CreateSectionFromTemplateRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteSectionResponseDTO;
import in.gram.gov.app.egram_service.dto.response.SectionTemplateResponseDTO;
import in.gram.gov.app.egram_service.facade.SectionTemplateFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/panchayat/website/templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
@Slf4j
public class SectionTemplateController {
    private final SectionTemplateFacade facade;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<SectionTemplateResponseDTO>>>> getTemplates(
            @RequestParam(required = false) String language) {
        log.info("SectionTemplateController.getTemplates called - language={}", language);
        List<SectionTemplateResponseDTO> templates = facade.getAllTemplates(language);
        return ResponseEntity.ok(ApiResponse.success(Map.of("templates", templates)));
    }

    @PostMapping("/{templateId}/create-page")
    public ResponseEntity<ApiResponse<Map<String, List<PanchayatWebsiteSectionResponseDTO>>>> createPageFromTemplate(
            @PathVariable Long templateId) {
        log.info("SectionTemplateController.createPageFromTemplate called - templateId={}", templateId);
        List<PanchayatWebsiteSectionResponseDTO> sections = facade.createPageFromTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("sections", sections)));
    }

    @PostMapping("/{templateId}/create-section")
    public ResponseEntity<ApiResponse<PanchayatWebsiteSectionResponseDTO>> createSectionFromTemplate(
            @PathVariable Long templateId,
            @RequestBody(required = false) CreateSectionFromTemplateRequestDTO request) {
        log.info("SectionTemplateController.createSectionFromTemplate called - templateId={}", templateId);
        
        Integer displayOrder = request != null ? request.getDisplayOrder() : null;
        Boolean isVisible = request != null && request.getIsVisible() != null ? request.getIsVisible() : true;
        
        PanchayatWebsiteSectionResponseDTO section = facade.createSectionFromTemplate(templateId, displayOrder, isVisible);
        return ResponseEntity.ok(ApiResponse.success(section));
    }
}

