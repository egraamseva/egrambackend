package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.constants.enums.SchemeStatus;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.request.SchemeRequestDTO;
import in.gram.gov.app.egram_service.dto.response.SchemeResponseDTO;
import in.gram.gov.app.egram_service.facade.SchemeFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/panchayat/schemes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
@Slf4j
public class PanchayatSchemeController {
    private final SchemeFacade schemeFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<SchemeResponseDTO>> create(
            @Valid @RequestBody SchemeRequestDTO request,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("PanchayatSchemeController.create called - email={}, request={}", email, request);
        SchemeResponseDTO response = schemeFacade.create(request, email);
        return ResponseEntity.ok(ApiResponse.success("Scheme created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SchemeResponseDTO>>> getAll(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        log.info("PanchayatSchemeController.getAll called - page={}, size={}", page, size);
        Page<SchemeResponseDTO> schemes = schemeFacade.getAll(page, size);
        PagedResponse<SchemeResponseDTO> response = PagedResponse.of(schemes);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchemeResponseDTO>> getById(@PathVariable Long id) {
        log.info("PanchayatSchemeController.getById called - id={}", id);
        SchemeResponseDTO response = schemeFacade.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SchemeResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody SchemeRequestDTO request) {
        log.info("PanchayatSchemeController.update called - id={}, request={}", id, request);
        SchemeResponseDTO response = schemeFacade.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Scheme updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam SchemeStatus status) {
        log.info("PanchayatSchemeController.updateStatus called - id={}, status={}", id, status);
        schemeFacade.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Scheme status updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        log.info("PanchayatSchemeController.delete called - id={}", id);
        schemeFacade.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Scheme deleted successfully", null));
    }
}
