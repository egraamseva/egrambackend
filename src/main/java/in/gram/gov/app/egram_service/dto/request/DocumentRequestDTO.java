package in.gram.gov.app.egram_service.dto.request;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentRequestDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 300)
    private String title;

    private String fileUrl; // Legacy field, optional now

    private DocumentCategory category;
    private String description;
    private Long fileSize;
    private String mimeType;
    private String fileName;
    
    // Google Drive fields
    private String googleDriveFileId;
    
    // Visibility
    private Visibility visibility = Visibility.PRIVATE;
    
    // File upload (for Google Drive)
    private MultipartFile file;
}

