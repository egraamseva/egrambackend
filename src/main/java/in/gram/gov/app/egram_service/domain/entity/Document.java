package in.gram.gov.app.egram_service.domain.entity;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "documents",
        indexes = {
                @Index(name = "idx_document_panchayat", columnList = "panchayat_id"),
                @Index(name = "idx_document_category", columnList = "category"),
                @Index(name = "idx_document_uploaded_by", columnList = "uploaded_by_user_id"),
                @Index(name = "idx_document_visibility", columnList = "visibility"),
                @Index(name = "idx_document_drive_id", columnList = "google_drive_file_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panchayat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_document_panchayat"))
    private Panchayat panchayat;

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "file_url", length = 500)
    private String fileUrl; // Legacy field, kept for backward compatibility

    @Column(name = "google_drive_file_id", unique = true, length = 255)
    private String googleDriveFileId; // Google Drive file ID

    @Column(name = "file_name", length = 500)
    private String fileName; // Original file name

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    @Builder.Default
    private DocumentCategory category = DocumentCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_document_uploader"))
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_id", foreignKey = @ForeignKey(name = "fk_document_consent"))
    private UserConsent consent; // Reference to consent used for this upload

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "download_count")
    @Builder.Default
    private Long downloadCount = 0L;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true; // False if file was deleted from Drive or access revoked

    @Column(name = "show_on_website", nullable = false)
    @Builder.Default
    private Boolean showOnWebsite = false; // Whether to display this document on public website
}