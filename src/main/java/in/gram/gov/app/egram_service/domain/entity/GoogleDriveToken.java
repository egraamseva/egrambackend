package in.gram.gov.app.egram_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "google_drive_tokens",
        indexes = {
                @Index(name = "idx_token_panchayat", columnList = "panchayat_id", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleDriveToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panchayat_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_token_panchayat"))
    private Panchayat panchayat;

    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken; // Encrypted

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken; // Encrypted

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "scope", length = 500)
    @Builder.Default
    private String scope = "https://www.googleapis.com/auth/drive.file";

    @Column(name = "token_type", length = 50)
    @Builder.Default
    private String tokenType = "Bearer";

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public boolean needsRefresh() {
        // Refresh if token expires in less than 5 minutes
        return LocalDateTime.now().isAfter(expiryTime.minusMinutes(5));
    }
}

