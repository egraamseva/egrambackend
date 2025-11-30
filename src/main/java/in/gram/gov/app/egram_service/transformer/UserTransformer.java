package in.gram.gov.app.egram_service.transformer;

import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.dto.request.UserRequestDTO;
import in.gram.gov.app.egram_service.dto.response.UserResponseDTO;

public class UserTransformer {

    public static UserResponseDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setDesignation(user.getDesignation());
        dto.setImageUrl(user.getImageUrl());
        dto.setImageKey(user.getImageKey());
        dto.setHasImage(user.getHasImage() != null && user.getHasImage());
        dto.setInitials(generateInitials(user.getName()));
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        if (user.getPanchayat() != null) {
            dto.setPanchayatId(user.getPanchayat().getId());
            dto.setPanchayatName(user.getPanchayat().getPanchayatName());
        }
        
        return dto;
    }

    /**
     * Generate initials from name (e.g., "John Doe" -> "JD", "Raj" -> "R")
     */
    private static String generateInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) {
            return "";
        } else if (parts.length == 1) {
            return parts[0].substring(0, Math.min(1, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, Math.min(1, parts[0].length())) +
                    parts[parts.length - 1].substring(0, Math.min(1, parts[parts.length - 1].length())))
                    .toUpperCase();
        }
    }

    public static User toEntity(UserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .designation(dto.getDesignation())
                .build();
    }
}

