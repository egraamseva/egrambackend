package in.gram.gov.app.egram_service.dto.response;

import in.gram.gov.app.egram_service.constants.enums.LayoutType;
import in.gram.gov.app.egram_service.constants.enums.SectionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlatformSectionResponseDTO {
    private Long id;
    private SectionType sectionType;
    private String title;
    private String subtitle;
    private String content; // JSON string
    private LayoutType layoutType;
    private Integer displayOrder;
    private Boolean isVisible;
    private String backgroundColor;
    private String textColor;
    private String imageUrl;
    private String imageKey;
    private String metadata; // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

