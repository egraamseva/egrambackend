package in.gram.gov.app.egram_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Album Request DTO
 * Supports both file upload and URL-only workflows for cover image
 * - File Upload: Provide coverImageFile with optional compressionQuality
 * - URL Only: Provide coverImageUrl string for existing/external URLs
 */
@Data
public class AlbumRequestDTO {
    @NotBlank(message = "Album name is required")
    @Size(max = 200)
    private String albumName;

    private String description;

    // Option 1: Upload new cover image file
    private MultipartFile coverImageFile;
    private String compressionQuality; // HIGH, MEDIUM, LOW

    // Option 2: Use existing URL (backward compatibility)
    private String coverImageUrl;
}

