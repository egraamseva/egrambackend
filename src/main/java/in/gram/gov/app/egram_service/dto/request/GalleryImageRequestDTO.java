package in.gram.gov.app.egram_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Gallery Image Request DTO
 * Supports both file upload and URL-only workflows
 * - File Upload: Provide imageFile with optional compressionQuality
 * - URL Only: Provide imageUrl string for existing/external URLs
 */
@Data
public class GalleryImageRequestDTO {
    // Option 1: Upload new image file
    private MultipartFile imageFile;
    private String compressionQuality; // HIGH, MEDIUM, LOW

    // Option 2: Use existing URL (backward compatibility)
    private String imageUrl;

    @Size(max = 500)
    private String caption;

    private String tags;
    private Long albumId;
    private Integer displayOrder;
}

