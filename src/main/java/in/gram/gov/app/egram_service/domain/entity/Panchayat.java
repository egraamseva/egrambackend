package in.gram.gov.app.egram_service.domain.entity;


import in.gram.gov.app.egram_service.constants.enums.PanchayatStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "panchayats",
        indexes = {
                @Index(name = "idx_panchayat_slug", columnList = "slug"),
                @Index(name = "idx_panchayat_status", columnList = "status"),
                @Index(name = "idx_panchayat_district_state", columnList = "district, state")
        })
@SQLDelete(sql = "UPDATE panchayats SET status = 'INACTIVE' WHERE id = ?")
@Where(clause = "status != 'DELETED'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panchayat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Panchayat name is required")
    @Size(max = 200, message = "Panchayat name must not exceed 200 characters")
    @Column(name = "panchayat_name", nullable = false, length = 200)
    private String panchayatName;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @NotBlank(message = "District is required")
    @Size(max = 100)
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    @Email(message = "Invalid email format")
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "about_text", columnDefinition = "TEXT")
    private String aboutText;

    @Size(max = 200)
    @Column(name = "hero_title", length = 200)
    private String heroTitle;

    @Size(max = 200)
    @Column(name = "hero_subtitle", length = 200)
    private String heroSubtitle;

    @Size(max = 200)
    @Column(name = "about_title", length = 200)
    private String aboutTitle;

    @Column(name = "about_features", columnDefinition = "TEXT")
    private String aboutFeatures; // JSON array stored as string

    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "office_phone", length = 15)
    private String officePhone;

    @Email
    @Column(name = "office_email", length = 100)
    private String officeEmail;

    @Column(name = "map_coordinates", length = 100)
    private String mapCoordinates;

    @Column(name = "office_hours", length = 200)
    private String officeHours;

    @Column(name = "population")
    private Long population;

    @Column(name = "area", length = 50)
    private String area;

    @Column(name = "wards")
    private Integer wards;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PanchayatStatus status = PanchayatStatus.ACTIVE;

    // Relationships
    @OneToMany(mappedBy = "panchayat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "panchayat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "panchayat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Announcement> announcements = new ArrayList<>();

    @OneToMany(mappedBy = "panchayat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Scheme> schemes = new ArrayList<>();

    @OneToMany(mappedBy = "panchayat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GalleryImage> galleryImages = new ArrayList<>();

    @OneToMany(mappedBy = "panchayat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.slug != null) {
            this.slug = this.slug.toLowerCase().trim();
        }
    }
}