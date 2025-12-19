package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.request.DocumentRequestDTO;
import in.gram.gov.app.egram_service.dto.response.DocumentResponseDTO;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.facade.DocumentFacade;
import in.gram.gov.app.egram_service.service.GoogleDriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/panchayat/documents")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
public class PanchayatDocumentController {
    private final DocumentFacade documentFacade;
    private final GoogleDriveService googleDriveService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> uploadDocument(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam DocumentCategory category,
            @RequestParam(required = false, defaultValue = "PRIVATE") Visibility visibility,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.uploadDocument called - title={}, category={}", 
                title, category);
        
        String email = authentication.getName();
        
        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        request.setVisibility(visibility);
        request.setFile(file);
        
        DocumentResponseDTO response = documentFacade.uploadDocument(request, email);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DocumentResponseDTO>>> getDocuments(
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(required = false) Visibility visibility,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        log.info("PanchayatDocumentController.getDocuments called - category={}, visibility={}", 
                category, visibility);
        
        String email = authentication.getName();
        PagedResponse<DocumentResponseDTO> response = documentFacade.getDocuments(
                category, visibility, pageable, email);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> getDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.getDocument called - id={}", id);
        
        String email = authentication.getName();
        DocumentResponseDTO response = documentFacade.getById(id, email);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> getDocumentView(
            @PathVariable Long id,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.getDocumentView called - id={}", id);
        
        String email = authentication.getName();
        DocumentResponseDTO response = documentFacade.getById(id, email);
        
        // Ensure file permissions are correct before returning view link
        if (response.getGoogleDriveFileId() != null) {
            try {
                Long panchayatId = TenantContext.getTenantId();
                if (panchayatId != null) {
                    googleDriveService.updateFilePermissions(response.getGoogleDriveFileId(), panchayatId);
                }
            } catch (Exception e) {
                log.warn("Failed to update file permissions when getting view link: {}", e.getMessage());
                // Continue - view link might still work
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> updateVisibility(
            @PathVariable Long id,
            @RequestParam Visibility visibility,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.updateVisibility called - id={}, visibility={}", 
                id, visibility);
        
        String email = authentication.getName();
        DocumentResponseDTO response = documentFacade.updateVisibility(id, visibility, email);
        
        return ResponseEntity.ok(ApiResponse.success("Visibility updated successfully", response));
    }

    @PatchMapping("/{id}/show-on-website")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> toggleShowOnWebsite(
            @PathVariable Long id,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.toggleShowOnWebsite called - id={}", id);
        
        String email = authentication.getName();
        DocumentResponseDTO response = documentFacade.toggleShowOnWebsite(id, email);
        
        return ResponseEntity.ok(ApiResponse.success("Show on website status updated", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> updateDocument(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam DocumentCategory category,
            @RequestParam(required = false, defaultValue = "PRIVATE") Visibility visibility,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.updateDocument called - id={}, title={}", id, title);
        
        String email = authentication.getName();
        
        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        request.setVisibility(visibility);
        
        DocumentResponseDTO response = documentFacade.updateDocument(id, request, email);
        return ResponseEntity.ok(ApiResponse.success("Document updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {
        
        log.info("PanchayatDocumentController.deleteDocument called - id={}", id);
        
        String email = authentication.getName();
        documentFacade.deleteDocument(id, email);
        
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
}

