package in.gram.gov.app.egram_service.domain.entity;

import in.gram.gov.app.egram_service.constants.enums.UserRole;
import in.gram.gov.app.egram_service.constants.enums.UserStatus;
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
@Table(name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_panchayat", columnList = "panchayat_id"),
                @Index(name = "idx_user_role_status", columnList = "role, status")
        })
@SQLDelete(sql = "UPDATE users SET status = 'INACTIVE' WHERE id = ?")
@Where(clause = "status != 'DELETED'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    @Column(name = "phone", length = 15)
    private String phone;

    @Size(max = 100)
    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(name = "has_image")
    @Builder.Default
    private Boolean hasImage = false;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.PANCHAYAT_ADMIN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panchayat_id", foreignKey = @ForeignKey(name = "fk_user_panchayat"))
    private Panchayat panchayat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login")
    private java.time.LocalDateTime lastLogin;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expiry")
    private java.time.LocalDateTime passwordResetExpiry;

    // Relationships
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "uploadedBy", cascade = CascadeType.ALL)
    @Builder.Default
    private List<GalleryImage> uploadedImages = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Announcement> announcements = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Scheme> schemes = new ArrayList<>();
}
