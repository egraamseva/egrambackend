package in.gram.gov.app.egram_service.dto.response;

import in.gram.gov.app.egram_service.constants.enums.UserRole;
import in.gram.gov.app.egram_service.constants.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String designation;
    private String imageUrl;
    private String imageKey;
    private Boolean hasImage;
    private String initials; // Generated from name (e.g., "John Doe" -> "JD")
    private UserRole role;
    private UserStatus status;
    private Long panchayatId;
    private String panchayatName;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

