package in.gram.gov.app.egram_service.dto.response;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponseDTO {
    private Long documentId;
    private String title;
    private String fileUrl; // Legacy field
    private String fileName;
    private String googleDriveFileId;
    private String viewLink; // Google Drive preview URL
    private DocumentCategory category;
    private Visibility visibility;
    private String description;
    private Long fileSize;
    private String mimeType;
    private Long downloadCount;
    private Boolean isAvailable;
    private Long uploadedByUserId;
    private String uploadedByName;
    private Boolean showOnWebsite; // Whether to display on public website
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

