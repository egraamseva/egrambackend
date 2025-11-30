package in.gram.gov.app.egram_service.controller.panchayat;

import in.gram.gov.app.egram_service.constants.enums.UserStatus;
import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.request.StatusUpdateRequestDTO;
import in.gram.gov.app.egram_service.dto.request.UserRequestDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;
import in.gram.gov.app.egram_service.facade.UserFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/panchayat/team")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PANCHAYAT_ADMIN')")
@Slf4j
public class PanchayatTeamController {
    private final UserFacade userFacade;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponseDTO>> addTeamMember(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String imageUrl) {
        log.info("PanchayatTeamController.addTeamMember called");
        
        UserRequestDTO request = new UserRequestDTO();
        request.setName(name);
        request.setEmail(email);
        request.setPhone(phone);
        request.setDesignation(designation);
        request.setPassword(password);
        request.setImageFile(imageFile);
        request.setImageUrl(imageUrl);
        request.setCompressionQuality(compressionQuality);
        
        UserResponseDTO response = userFacade.addTeamMember(request);
        return ResponseEntity.ok(ApiResponse.success("Team member added successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDTO>>> getTeamMembers(
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("PanchayatTeamController.getTeamMembers called - pageable={}", pageable);
        PagedResponse<UserResponseDTO> response = PagedResponse.of(userFacade.getTeamMembers(pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Object>> removeTeamMember(@PathVariable Long userId) {
        log.info("PanchayatTeamController.removeTeamMember called - userId={}", userId);
        userFacade.removeTeamMember(userId);
        return ResponseEntity.ok(ApiResponse.success("Team member removed successfully", null));
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateTeamMember(
            @PathVariable Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false, defaultValue = "HIGH") String compressionQuality,
            @RequestParam(required = false) String imageUrl) {
        log.info("PanchayatTeamController.updateTeamMember called - userId={}", userId);
        
        UserRequestDTO request = new UserRequestDTO();
        request.setName(name);
        request.setEmail(email);
        request.setPhone(phone);
        request.setDesignation(designation);
        request.setPassword(password);
        request.setImageFile(imageFile);
        request.setImageUrl(imageUrl);
        request.setCompressionQuality(compressionQuality);
        
        UserResponseDTO response = userFacade.updateTeamMember(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Team member updated successfully", response));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Object>> updateTeamMemberStatus(
            @PathVariable Long userId,
            @Valid @RequestBody StatusUpdateRequestDTO request) {
        log.info("PanchayatTeamController.updateTeamMemberStatus called - userId={}, request={}", userId, request);
        UserStatus status = UserStatus.valueOf(request.getStatus().toUpperCase());
        userFacade.updateTeamMemberStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", null));
    }
}
