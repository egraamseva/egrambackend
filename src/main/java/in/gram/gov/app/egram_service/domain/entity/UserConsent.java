package in.gram.gov.app.egram_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_consents",
        indexes = {
                @Index(name = "idx_consent_user", columnList = "user_id"),
                @Index(name = "idx_consent_active", columnList = "user_id, revoked_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_consent_user"))
    private User user;

    @Column(name = "consent_given", nullable = false)
    @Builder.Default
    private Boolean consentGiven = false;

    @Column(name = "consent_timestamp", nullable = false)
    private LocalDateTime consentTimestamp;

    @Column(name = "purpose", length = 500)
    @Builder.Default
    private String purpose = "Google Drive document storage for Panchayat documents";

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    public boolean isActive() {
        return consentGiven && revokedAt == null;
    }
}

