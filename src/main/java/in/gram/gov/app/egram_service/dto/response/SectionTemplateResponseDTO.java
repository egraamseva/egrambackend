package in.gram.gov.app.egram_service.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SectionTemplateResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String language;
    private Object templateData; // Parsed JSON object
    private String colorTheme;
    private String previewImageUrl;
    private Boolean isActive;
    private Boolean isSystem;
    private Boolean isPageTemplate;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

