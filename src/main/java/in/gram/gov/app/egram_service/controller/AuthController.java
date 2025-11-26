package in.gram.gov.app.egram_service.controller;

import in.gram.gov.app.egram_service.dto.ApiResponse;
import in.gram.gov.app.egram_service.dto.request.*;
import in.gram.gov.app.egram_service.dto.response.LoginResponseDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;
import in.gram.gov.app.egram_service.facade.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthFacade authFacade;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("AuthController.register called - request={}", request);
        LoginResponseDTO response = authFacade.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("AuthController.login called - request={}", request);
        LoginResponseDTO response = authFacade.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout() {
        log.info("AuthController.logout called");
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        log.info("AuthController.forgotPassword called - request={}", request);
        authFacade.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        log.info("AuthController.resetPassword called - request={}", request);
        authFacade.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.info("AuthController.getCurrentUser called - email={}", email);
        UserResponseDTO response = authFacade.getCurrentUser(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @Valid @RequestBody UserRequestDTO request,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("AuthController.updateProfile called - email={}, request={}", email, request);
        UserResponseDTO response = authFacade.updateProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication) {
        String email = authentication.getName();
        log.info("AuthController.changePassword called - email={}", email);
        authFacade.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
