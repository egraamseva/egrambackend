package in.gram.gov.app.egram_service.dto.request;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class NewsletterRequestDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @Size(max = 500, message = "Subtitle must not exceed 500 characters")
    private String subtitle;

    @Size(max = 500)
    private String coverImageFileKey; // For URL-based workflow

    // Optional cover image file upload
    private MultipartFile coverImageFile;

    // Compression quality for uploaded cover image (default: HIGH)
    private CompressionQuality compressionQuality = CompressionQuality.HIGH;

    private String content; // Rich text / HTML / JSON blocks

    private List<String> bulletPoints = new ArrayList<>();

    private LocalDate publishedOn;

    @Size(max = 100, message = "Author name must not exceed 100 characters")
    private String authorName;

    private List<String> attachments = new ArrayList<>(); // List of file keys

    private Boolean isPublished = false;
}

