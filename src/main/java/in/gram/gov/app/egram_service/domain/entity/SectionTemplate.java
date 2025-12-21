package in.gram.gov.app.egram_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "section_templates",
        indexes = {
                @Index(name = "idx_section_template_language", columnList = "language"),
                @Index(name = "idx_section_template_active", columnList = "is_active, language"),
                @Index(name = "idx_section_template_page", columnList = "is_page_template, language")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "template_data", columnDefinition = "TEXT", nullable = false)
    private String templateData; // JSON string containing section configuration

    @Column(name = "color_theme", length = 50)
    private String colorTheme;

    @Column(name = "preview_image_url", length = 500)
    private String previewImageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_page_template", nullable = false)
    @Builder.Default
    private Boolean isPageTemplate = false;

    @Column(name = "display_order")
    private Integer displayOrder;
}

