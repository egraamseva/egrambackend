package in.gram.gov.app.egram_service.domain.entity;

import in.gram.gov.app.egram_service.constants.enums.LayoutType;
import in.gram.gov.app.egram_service.constants.enums.SectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "panchayat_website_sections",
        indexes = {
                @Index(name = "idx_panchayat_section_panchayat", columnList = "panchayat_id"),
                @Index(name = "idx_panchayat_section_order", columnList = "panchayat_id, display_order"),
                @Index(name = "idx_panchayat_section_visible", columnList = "panchayat_id, is_visible, display_order")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanchayatWebsiteSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panchayat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_panchayat_website_section_panchayat"))
    private Panchayat panchayat;

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

