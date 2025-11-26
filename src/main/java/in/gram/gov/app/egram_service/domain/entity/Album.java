
// ============================================
// 7. Album Entity
// ============================================
package in.gram.gov.app.egram_service.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "albums",
        indexes = {
                @Index(name = "idx_album_panchayat", columnList = "panchayat_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Album extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panchayat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_album_panchayat"))
    private Panchayat panchayat;

    @NotBlank(message = "Album name is required")
    @Size(max = 200)
    @Column(name = "album_name", nullable = false, length = 200)
    private String albumName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    // Relationships
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GalleryImage> images = new ArrayList<>();
}