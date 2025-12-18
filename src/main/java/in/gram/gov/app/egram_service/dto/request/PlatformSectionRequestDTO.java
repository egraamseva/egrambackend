package in.gram.gov.app.egram_service.dto.request;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.enums.LayoutType;
import in.gram.gov.app.egram_service.constants.enums.SectionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PlatformSectionRequestDTO {
    // Note: @NotNull removed to allow partial updates. Validation is handled in facade for create operations.
    private SectionType sectionType;

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 1000, message = "Subtitle must not exceed 1000 characters")
    private String subtitle;

    private String content; // JSON string

    // Note: @NotNull removed to allow partial updates. Validation is handled in facade for create operations.
    private LayoutType layoutType;

    private Integer displayOrder;

    private Boolean isVisible;

    @Size(max = 50, message = "Background color must not exceed 50 characters")
    private String backgroundColor;

    @Size(max = 50, message = "Text color must not exceed 50 characters")
    private String textColor;

    private String imageUrl;

    private String imageKey;

    private String metadata; // JSON string

    // Optional image file upload
    private MultipartFile imageFile;

    // Compression quality for uploaded image (default: HIGH)
    private CompressionQuality compressionQuality = CompressionQuality.HIGH;

    // Optional content item images (for items within the section content)
    private java.util.List<MultipartFile> contentItemImages;
}

