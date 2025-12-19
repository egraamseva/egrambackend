package in.gram.gov.app.egram_service.transformer;

import in.gram.gov.app.egram_service.domain.entity.Document;
import in.gram.gov.app.egram_service.dto.request.DocumentRequestDTO;
import in.gram.gov.app.egram_service.dto.response.DocumentResponseDTO;

public class DocumentTransformer {

    public static DocumentResponseDTO toDTO(Document document) {
        if (document == null) {
            return null;
        }
        
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setDocumentId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setFileUrl(document.getFileUrl());
        dto.setFileName(document.getFileName());
        dto.setGoogleDriveFileId(document.getGoogleDriveFileId());
        dto.setCategory(document.getCategory());
        dto.setVisibility(document.getVisibility());
        dto.setDescription(document.getDescription());
        dto.setFileSize(document.getFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setDownloadCount(document.getDownloadCount());
        dto.setIsAvailable(document.getIsAvailable());
        dto.setShowOnWebsite(document.getShowOnWebsite());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        
        if (document.getUploadedBy() != null) {
            dto.setUploadedByUserId(document.getUploadedBy().getId());
            dto.setUploadedByName(document.getUploadedBy().getName());
        }
        
        return dto;
    }

    public static DocumentResponseDTO toDTO(Document document, String viewLink) {
        DocumentResponseDTO dto = toDTO(document);
        if (dto != null) {
            dto.setViewLink(viewLink);
        }
        return dto;
    }

    public static Document toEntity(DocumentRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Document.builder()
                .title(dto.getTitle())
                .fileUrl(dto.getFileUrl())
                .fileName(dto.getFileName())
                .googleDriveFileId(dto.getGoogleDriveFileId())
                .category(dto.getCategory())
                .visibility(dto.getVisibility() != null ? dto.getVisibility() : in.gram.gov.app.egram_service.constants.enums.Visibility.PRIVATE)
                .description(dto.getDescription())
                .fileSize(dto.getFileSize())
                .mimeType(dto.getMimeType())
                .build();
    }
}

