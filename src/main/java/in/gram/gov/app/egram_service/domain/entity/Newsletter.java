package in.gram.gov.app.egram_service.domain.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Newsletter Entity
 * Represents newsletters published by Panchayats
 */
@Entity
@Table(name = "newsletters",
        indexes = {
                @Index(name = "idx_newsletter_panchayat", columnList = "panchayat_id"),
                @Index(name = "idx_newsletter_published", columnList = "is_published, published_on"),
                @Index(name = "idx_newsletter_panchayat_published", columnList = "panchayat_id, is_published, published_on")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Newsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panchayat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_newsletter_panchayat"))
    private Panchayat panchayat;

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Size(max = 500)
    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "cover_image_file_key", length = 500)
    private String coverImageFileKey;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // Rich text / HTML / JSON blocks

    @Type(JsonType.class)
    @Column(name = "bullet_points", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> bulletPoints = new ArrayList<>();

    @Column(name = "published_on")
    private LocalDate publishedOn;

    @Size(max = 100)
    @Column(name = "author_name", length = 100)
    private String authorName;

    @Type(JsonType.class)
    @Column(name = "attachments", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> attachments = new ArrayList<>(); // List of file keys

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_newsletter_creator"))
    private User createdBy;
}

