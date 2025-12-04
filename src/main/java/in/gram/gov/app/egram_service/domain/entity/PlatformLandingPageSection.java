package in.gram.gov.app.egram_service.domain.entity;

import in.gram.gov.app.egram_service.constants.enums.LayoutType;
import in.gram.gov.app.egram_service.constants.enums.SectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "platform_landing_page_sections",
        indexes = {
                @Index(name = "idx_platform_section_order", columnList = "display_order"),
                @Index(name = "idx_platform_section_visible", columnList = "is_visible, display_order")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformLandingPageSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 50)
    private SectionType sectionType;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "subtitle", length = 1000)
    private String subtitle;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // JSON string

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", nullable = false, length = 20)
    @Builder.Default
    private LayoutType layoutType = LayoutType.GRID;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    @Column(name = "background_color", length = 50)
    private String backgroundColor;

    @Column(name = "text_color", length = 50)
    private String textColor;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for section-specific config
}

