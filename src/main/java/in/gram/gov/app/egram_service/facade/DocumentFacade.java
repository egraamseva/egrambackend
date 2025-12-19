package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import in.gram.gov.app.egram_service.constants.exception.BadRequestException;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Document;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.domain.entity.UserConsent;
import in.gram.gov.app.egram_service.dto.PagedResponse;
import in.gram.gov.app.egram_service.dto.request.DocumentRequestDTO;
import in.gram.gov.app.egram_service.dto.response.DocumentResponseDTO;
import in.gram.gov.app.egram_service.service.*;
import in.gram.gov.app.egram_service.transformer.DocumentTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentFacade {
    private final DocumentService documentService;
    private final PanchayatService panchayatService;
    private final UserService userService;
    private final GoogleDriveService googleDriveService;
    private final ConsentService consentService;

    @Transactional
    public DocumentResponseDTO uploadDocument(DocumentRequestDTO request, String email) throws IOException {
        log.info("DocumentFacade.uploadDocument called - title={}, email={}", request.getTitle(), email);
        
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);
        User user = userService.findByEmail(email);

        // Validate consent
        if (!consentService.hasValidConsent(user.getId())) {
            throw new BadRequestException("User consent is required before uploading documents");
        }

        // Validate file
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        validateFile(file);

        // Upload to Google Drive (use panchayat ID - panchayat email should be used)
        String googleDriveFileId = googleDriveService.uploadFile(
                file, 
                panchayat.getId(), 
                request.getCategory() != null ? request.getCategory().name() : "OTHER"
        );

        // Get active consent
        UserConsent consent = consentService.getActiveConsent(user.getId());

        // Create document entity
        Document document = Document.builder()
                .title(request.getTitle())
                .fileName(file.getOriginalFilename())
                .googleDriveFileId(googleDriveFileId)
                .category(request.getCategory() != null ? request.getCategory() : DocumentCategory.OTHER)
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE)
                .description(request.getDescription())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .panchayat(panchayat)
                .uploadedBy(user)
                .consent(consent)
                .isAvailable(true)
                .downloadCount(0L)
                .build();

        document = documentService.create(document);

        // Ensure file permissions are set correctly (in case sharing failed during upload)
        try {
            googleDriveService.updateFilePermissions(googleDriveFileId, panchayat.getId());
        } catch (Exception e) {
            log.warn("Failed to update file permissions after upload, but document was created. fileId={}, error={}", 
                    googleDriveFileId, e.getMessage());
        }

        // Get view link (use panchayat ID)
        String viewLink = googleDriveService.createViewLink(googleDriveFileId, panchayat.getId());

        log.info("DocumentFacade.uploadDocument - document created successfully. id={}, fileId={}", 
                document.getId(), googleDriveFileId);
        
        return DocumentTransformer.toDTO(document, viewLink);
    }

    @Transactional
    public DocumentResponseDTO updateDocument(Long id, DocumentRequestDTO request, String email) throws IOException {
        log.info("DocumentFacade.updateDocument called - id={}, title={}, email={}", 
                id, request.getTitle(), email);
        
        Document document = documentService.findById(id);
        User user = userService.findByEmail(email);

        // Check ownership
        if (!document.getUploadedBy().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to modify this document");
        }

        // Update document fields
        document.setTitle(request.getTitle());
        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            document.setCategory(request.getCategory());
        }
        if (request.getVisibility() != null) {
            document.setVisibility(request.getVisibility());
        }

        document = documentService.update(document);

        // Get view link
        String viewLink = null;
        if (document.getGoogleDriveFileId() != null && document.getIsAvailable()) {
            try {
                viewLink = googleDriveService.createViewLink(document.getGoogleDriveFileId(), document.getPanchayat().getId());
            } catch (Exception e) {
                log.warn("Failed to get view link: {}", e.getMessage());
            }
        }

        log.info("DocumentFacade.updateDocument - document updated successfully. id={}", document.getId());
        return DocumentTransformer.toDTO(document, viewLink);
    }

    public DocumentResponseDTO getById(Long id, String email) throws IOException {
        log.debug("DocumentFacade.getById called - id={}, email={}", id, email);
        
        Document document = documentService.findById(id);
        User user = userService.findByEmail(email);

        // Check access permissions
        if (document.getVisibility() == Visibility.PRIVATE && 
            !document.getUploadedBy().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this document");
        }

        String viewLink = null;
        if (document.getGoogleDriveFileId() != null && document.getIsAvailable()) {
            try {
                // Use panchayat ID for Google Drive operations (panchayat email should be used)
                viewLink = googleDriveService.createViewLink(document.getGoogleDriveFileId(), document.getPanchayat().getId());
            } catch (Exception e) {
                log.warn("Failed to get view link for document {}: {}", id, e.getMessage());
            }
        }

        return DocumentTransformer.toDTO(document, viewLink);
    }

    public PagedResponse<DocumentResponseDTO> getDocuments(
            DocumentCategory category, 
            Visibility visibility, 
            Pageable pageable, 
            String email) {
        log.info("DocumentFacade.getDocuments called - category={}, visibility={}, email={}", 
                category, visibility, email);
        
        Long tenantId = TenantContext.getTenantId();
        User user = userService.findByEmail(email);

        Page<Document> documents = documentService.findByPanchayatIdWithFilters(
                tenantId, category, visibility, pageable);

        // Filter and map to DTOs with view links
        List<DocumentResponseDTO> dtos = documents.getContent().stream()
                .map(doc -> {
                    try {
                        String viewLink = null;
                        if (doc.getGoogleDriveFileId() != null && doc.getIsAvailable()) {
                            // Only get view link if user has access
                            if (doc.getVisibility() == Visibility.PUBLIC || 
                                doc.getUploadedBy().getId().equals(user.getId())) {
                                try {
                                    // Use panchayat ID for Google Drive operations (panchayat email should be used)
                                    viewLink = googleDriveService.createViewLink(
                                            doc.getGoogleDriveFileId(), doc.getPanchayat().getId());
                                } catch (Exception e) {
                                    log.debug("Failed to get view link: {}", e.getMessage());
                                }
                            }
                        }
                        return DocumentTransformer.toDTO(doc, viewLink);
                    } catch (Exception e) {
                        log.warn("Error processing document {}: {}", doc.getId(), e.getMessage());
                        return DocumentTransformer.toDTO(doc);
                    }
                })
                .collect(Collectors.toList());

        // Create a new Page with the DTOs that have view links
        Page<DocumentResponseDTO> dtoPage = new PageImpl<>(dtos, pageable, documents.getTotalElements());
        return PagedResponse.of(dtoPage);
    }

    public PagedResponse<DocumentResponseDTO> getPublicDocuments(
            String slug, 
            DocumentCategory category, 
            Pageable pageable) {
        log.info("DocumentFacade.getPublicDocuments called - slug={}, category={}", slug, category);
        
        Page<Document> documents = documentService.findPublicDocumentsBySlug(slug, category, pageable);

        return PagedResponse.of(documents.map(DocumentTransformer::toDTO));
    }

    public PagedResponse<DocumentResponseDTO> getWebsiteDocuments(
            String slug, 
            DocumentCategory category, 
            Pageable pageable) {
        log.info("DocumentFacade.getWebsiteDocuments called - slug={}, category={}", slug, category);
        
        Page<Document> documents = documentService.findWebsiteDocumentsBySlug(slug, category, pageable);

        return PagedResponse.of(documents.map(DocumentTransformer::toDTO));
    }

    @Transactional
    public DocumentResponseDTO updateVisibility(Long id, Visibility visibility, String email) throws IOException {
        log.info("DocumentFacade.updateVisibility called - id={}, visibility={}, email={}", 
                id, visibility, email);
        
        Document document = documentService.findById(id);
        User user = userService.findByEmail(email);

        // Check ownership
        if (!document.getUploadedBy().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to modify this document");
        }

        document.setVisibility(visibility);
        document = documentService.update(document);

        String viewLink = null;
        if (document.getGoogleDriveFileId() != null && document.getIsAvailable()) {
            try {
                // Use panchayat ID for Google Drive operations (panchayat email should be used)
                viewLink = googleDriveService.createViewLink(document.getGoogleDriveFileId(), document.getPanchayat().getId());
            } catch (Exception e) {
                log.warn("Failed to get view link: {}", e.getMessage());
            }
        }

        return DocumentTransformer.toDTO(document, viewLink);
    }

    @Transactional
    public DocumentResponseDTO toggleShowOnWebsite(Long id, String email) throws IOException {
        log.info("DocumentFacade.toggleShowOnWebsite called - id={}, email={}", id, email);
        
        Document document = documentService.findById(id);
        User user = userService.findByEmail(email);

        // Check ownership
        if (!document.getUploadedBy().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to modify this document");
        }

        // Toggle showOnWebsite
        document.setShowOnWebsite(!document.getShowOnWebsite());
        document = documentService.update(document);

        String viewLink = null;
        if (document.getGoogleDriveFileId() != null && document.getIsAvailable()) {
            try {
                // Use panchayat ID for Google Drive operations (panchayat email should be used)
                viewLink = googleDriveService.createViewLink(document.getGoogleDriveFileId(), document.getPanchayat().getId());
            } catch (Exception e) {
                log.warn("Failed to get view link: {}", e.getMessage());
            }
        }

        return DocumentTransformer.toDTO(document, viewLink);
    }

    @Transactional
    public void deleteDocument(Long id, String email) throws IOException {
        log.info("DocumentFacade.deleteDocument called - id={}, email={}", id, email);
        
        Document document = documentService.findById(id);
        User user = userService.findByEmail(email);

        // Check ownership
        if (!document.getUploadedBy().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this document");
        }

        // Delete from Google Drive (use panchayat ID - panchayat email should be used)
        if (document.getGoogleDriveFileId() != null) {
            try {
                googleDriveService.deleteFile(document.getGoogleDriveFileId(), document.getPanchayat().getId());
            } catch (Exception e) {
                log.warn("Failed to delete file from Google Drive: {}", e.getMessage());
                // Continue with database deletion even if Drive deletion fails
            }
        }

        // Delete from database
        documentService.delete(id);
    }

    private void validateFile(MultipartFile file) {
        // Check file size (10MB max)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new BadRequestException("File size exceeds maximum allowed size of 10MB");
        }

        // Check file type
        String contentType = file.getContentType();
        String allowedTypes = "application/pdf,image/jpeg,image/png,application/msword," +
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document," +
                "application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException("File type not allowed. Allowed types: PDF, images, Word, Excel");
        }
    }
}

