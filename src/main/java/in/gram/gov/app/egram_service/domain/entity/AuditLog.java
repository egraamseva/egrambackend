package in.gram.gov.app.egram_service.domain.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import in.gram.gov.app.egram_service.constants.enums.ActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user", columnList = "user_id"),
                @Index(name = "idx_audit_panchayat", columnList = "panchayat_id"),
                @Index(name = "idx_audit_created_at", columnList = "created_at"),
                @Index(name = "idx_audit_action_type", columnList = "action_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_audit_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panchayat_id", foreignKey = @ForeignKey(name = "fk_audit_panchayat"))
    private Panchayat panchayat;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "target_entity_type", length = 50)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Type(JsonType.class)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "description", length = 1000)
    private String description;

    @PrePersist
    protected void onCreate() {
        // ...existing code...
    }
}