package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.enums.UserRole;
import in.gram.gov.app.egram_service.constants.enums.UserStatus;
import in.gram.gov.app.egram_service.constants.exception.MaxAdminsExceededException;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.dto.request.UserRequestDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;
import in.gram.gov.app.egram_service.service.*;
import in.gram.gov.app.egram_service.transformer.UserTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFacade {
    private final UserService userService;
    private final PanchayatService panchayatService;
    private final PasswordEncoder passwordEncoder;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;
    private static final int MAX_ADMINS = 4;

    @Transactional
    public UserResponseDTO addTeamMember(UserRequestDTO request) {
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);

        // Check max admins constraint
        Long activeAdminCount = userService.countByPanchayatIdAndRoleAndStatus(
                tenantId, UserRole.PANCHAYAT_ADMIN, UserStatus.ACTIVE);
        if (activeAdminCount >= MAX_ADMINS) {
            throw new MaxAdminsExceededException("Maximum " + MAX_ADMINS + " admins allowed per panchayat");
        }

        // Handle image upload if provided
        String imageUrl = request.getImageUrl();
        String imageKey = null;
        Boolean hasImage = false;
        
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
            String uploadedUrl = processAndUploadImage(request.getImageFile(), compressionQuality);
            if (uploadedUrl != null) {
                imageUrl = uploadedUrl;
                imageKey = extractFileKeyFromUrl(uploadedUrl);
                hasImage = true;
            }
        } else if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            imageKey = extractFileKeyFromUrl(request.getImageUrl());
            hasImage = true;
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .designation(request.getDesignation())
                .imageUrl(imageUrl)
                .imageKey(imageKey)
                .hasImage(hasImage)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.PANCHAYAT_ADMIN)
                .status(UserStatus.ACTIVE)
                .panchayat(panchayat)
                .build();

        user = userService.create(user);
        log.info("Team member added successfully with ID: {}", user.getId());
        return UserTransformer.toDTO(user);
    }

    @Transactional
    public UserResponseDTO updateTeamMember(Long userId, UserRequestDTO request) {
        Long tenantId = TenantContext.getTenantId();
        User user = userService.findById(userId);
        
        // Verify user belongs to current panchayat
        if (user.getPanchayat() == null || !user.getPanchayat().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Team member", userId);
        }

        // Update basic fields
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDesignation() != null) {
            user.setDesignation(request.getDesignation());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Handle image update
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            // Delete old image if exists
            if (user.getImageKey() != null) {
                try {
                    cloudStorageService.deleteImage(user.getImageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }
            
            CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
            String uploadedUrl = processAndUploadImage(request.getImageFile(), compressionQuality);
            if (uploadedUrl != null) {
                user.setImageUrl(uploadedUrl);
                user.setImageKey(extractFileKeyFromUrl(uploadedUrl));
                user.setHasImage(true);
            }
        } else if (request.getImageUrl() != null) {
            // Update with new URL
            if (request.getImageUrl().isEmpty()) {
                // Clear image
                if (user.getImageKey() != null) {
                    try {
                        cloudStorageService.deleteImage(user.getImageKey());
                    } catch (Exception e) {
                        log.warn("Failed to delete old image: {}", e.getMessage());
                    }
                }
                user.setImageUrl(null);
                user.setImageKey(null);
                user.setHasImage(false);
            } else {
                user.setImageUrl(request.getImageUrl());
                user.setImageKey(extractFileKeyFromUrl(request.getImageUrl()));
                user.setHasImage(true);
            }
        }

        user = userService.update(user);
        log.info("Team member updated successfully with ID: {}", user.getId());
        return UserTransformer.toDTO(user);
    }

    public Page<UserResponseDTO> getTeamMembers(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<User> users = userService.findByPanchayatId(tenantId, pageable);
        return users.map(UserTransformer::toDTO);
    }

    @Transactional
    public void removeTeamMember(Long userId) {
        userService.updateStatus(userId, UserStatus.INACTIVE);
    }

    @Transactional
    public void updateTeamMemberStatus(Long userId, UserStatus status) {
        userService.updateStatus(userId, status);
    }

    public Page<UserResponseDTO> getAll(Long panchayatId, UserRole role, UserStatus status, Pageable pageable) {
        Page<User> users = userService.findByFilters(panchayatId, role, status, pageable);
        return users.map(UserTransformer::toDTO);
    }

    public Page<UserResponseDTO> getTeamMembersBySlug(String slug, Pageable pageable) {
        Panchayat panchayat = panchayatService.findBySlug(slug);
        Page<User> users = userService.findByPanchayatId(panchayat.getId(), pageable);
        return users.map(UserTransformer::toDTO);
    }

    /**
     * Process image: compress and upload to Backblaze B2
     */
    private String processAndUploadImage(MultipartFile imageFile, CompressionQuality compressionQuality) {
        try {
            if (!cloudStorageService.isEnabled()) {
                log.warn("Cloud storage is disabled. Image upload skipped");
                return null;
            }

            if (compressionQuality == null) {
                compressionQuality = CompressionQuality.HIGH;
            }

            log.info("Processing team member image for upload: {}", imageFile.getOriginalFilename());

            // Step 1: Validate image
            imageCompressionService.validateImageFile(imageFile);

            // Step 2: Compress image
            ImageCompressionDTO compressionMetadata = imageCompressionService.compressImage(
                    imageFile,
                    compressionQuality
            );

            log.info("Image compressed successfully. Original: {} bytes, Compressed: {} bytes",
                    compressionMetadata.getOriginalFileSize(),
                    compressionMetadata.getCompressedFileSize());

            // Step 3: Get compressed image stream
            InputStream compressedImageStream = imageCompressionService.getCompressedImageInputStream(
                    imageFile,
                    compressionQuality
            );

            // Step 4: Upload to Backblaze B2
            ImageCompressionDTO uploadResult = cloudStorageService.uploadImage(
                    compressedImageStream,
                    compressionMetadata
            );

            log.info("Image uploaded to cloud storage. URL: {}", uploadResult.getBackblazeFileUrl());
            return uploadResult.getBackblazeFileUrl();

        } catch (Exception e) {
            log.error("Error processing image for upload", e);
            return null;
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL
     */
    private String extractFileKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            int fileIndex = imageUrl.indexOf("/file/");
            if (fileIndex == -1) {
                log.warn("Invalid image URL format - '/file/' not found: {}", imageUrl);
                return null;
            }

            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = imageUrl.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                log.warn("Cannot extract bucket name from URL: {}", imageUrl);
                return null;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = imageUrl.substring(bucketEnd + 1);

            if (fileKey.isEmpty()) {
                log.warn("Extracted file key is empty from URL: {}", imageUrl);
                return null;
            }

            // Remove query parameters if present (from presigned URLs)
            int queryIndex = fileKey.indexOf("?");
            if (queryIndex != -1) {
                fileKey = fileKey.substring(0, queryIndex);
            }

            return fileKey;

        } catch (Exception e) {
            log.error("Error extracting file key from URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * Parse compression quality from string
     */
    private CompressionQuality parseCompressionQuality(String quality) {
        if (quality == null || quality.isEmpty()) {
            return CompressionQuality.HIGH;
        }
        try {
            return CompressionQuality.valueOf(quality.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid compression quality: {}, using HIGH", quality);
            return CompressionQuality.HIGH;
        }
    }
}

