package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.UserRole;
import in.gram.gov.app.egram_service.constants.enums.UserStatus;
import in.gram.gov.app.egram_service.constants.exception.BadRequestException;
import in.gram.gov.app.egram_service.constants.exception.UnauthorizedException;
import in.gram.gov.app.egram_service.constants.security.JwtTokenProvider;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.dto.request.*;
import in.gram.gov.app.egram_service.dto.response.LoginResponseDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;
import in.gram.gov.app.egram_service.service.PanchayatService;
import in.gram.gov.app.egram_service.service.UserService;
import in.gram.gov.app.egram_service.transformer.UserTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthFacade {
    private final UserService userService;
    private final PanchayatService panchayatService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;




    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO request) {

        validateRegistrationRequest(request);

        User user = request.getRole() == UserRole.SUPER_ADMIN
                ? createSuperAdminUser(request)
                : createPanchayatAdminUser(request);

        return buildLoginResponse(user);
    }

    private void validateRegistrationRequest(RegisterRequestDTO request) {
        if (request.getRole() != null && request.getRole() != UserRole.SUPER_ADMIN) {
            throw new BadRequestException("Invalid role for registration");
        }

        User existingUser = userService.findByEmailOrNull(request.getEmail());
        if (existingUser != null) {
            throw new BadRequestException("User with this email already exists");
        }
    }

    private User createSuperAdminUser(RegisterRequestDTO request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        return userService.create(user);
    }

    private User createPanchayatAdminUser(RegisterRequestDTO request) {
        if (request.getPanchayatSlug() == null) {
            throw new BadRequestException("Panchayat slug is required for Panchayat Admin registration");
        }
        Panchayat panchayat = panchayatService.findBySlug(request.getPanchayatSlug());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.PANCHAYAT_ADMIN)
                .status(UserStatus.ACTIVE)
                .panchayat(panchayat)
                .build();

        return userService.create(user);
    }

    private LoginResponseDTO buildLoginResponse(User user) {
        // Generate token
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getPanchayat() != null ? user.getPanchayat().getId() : null,
                user.getRole().name(),
                user.getEmail()
        );

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUser(UserTransformer.toDTO(user));
        return response;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userService.findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userService.update(user);

        // Generate token
       return buildLoginResponse(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userService.findByEmailOrNull(request.getEmail());
        if (user != null) {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiry(LocalDateTime.now().plusHours(24));
            userService.update(user);
            // TODO: Send email with reset link
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        User user = userService.findByPasswordResetToken(request.getToken());

        if (user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userService.update(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequestDTO request) {
        User user = userService.findByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userService.update(user);
    }

    public UserResponseDTO getCurrentUser(String email) {
        User user = userService.findByEmail(email);
        return UserTransformer.toDTO(user);
    }

    @Transactional
    public UserResponseDTO updateProfile(String email, UserRequestDTO request) {
        User user = userService.findByEmail(email);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user = userService.update(user);
        return UserTransformer.toDTO(user);
    }
}

