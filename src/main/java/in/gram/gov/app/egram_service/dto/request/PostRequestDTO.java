package in.gram.gov.app.egram_service.dto.request;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PostRequestDTO {
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @NotBlank(message = "Body text is required")
    private String bodyText;

    private String mediaUrl;

    // Optional image file upload
    private MultipartFile imageFile;

    // Compression quality for uploaded image (default: HIGH)
    private CompressionQuality compressionQuality = CompressionQuality.HIGH;
}

