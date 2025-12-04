package in.gram.gov.app.egram_service.transformer;

import in.gram.gov.app.egram_service.domain.entity.PlatformLandingPageSection;
import in.gram.gov.app.egram_service.dto.request.PlatformSectionRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PlatformSectionResponseDTO;

public class PlatformLandingPageSectionTransformer {

    public static PlatformSectionResponseDTO toDTO(PlatformLandingPageSection section) {
        if (section == null) {
            return null;
        }
        
        PlatformSectionResponseDTO dto = new PlatformSectionResponseDTO();
        dto.setId(section.getId());
        dto.setSectionType(section.getSectionType());
        dto.setTitle(section.getTitle());
        dto.setSubtitle(section.getSubtitle());
        dto.setContent(section.getContent());
        dto.setLayoutType(section.getLayoutType());
        dto.setDisplayOrder(section.getDisplayOrder());
        dto.setIsVisible(section.getIsVisible());
        dto.setBackgroundColor(section.getBackgroundColor());
        dto.setTextColor(section.getTextColor());
        dto.setImageUrl(section.getImageUrl());
        dto.setImageKey(section.getImageKey());
        dto.setMetadata(section.getMetadata());
        dto.setCreatedAt(section.getCreatedAt());
        dto.setUpdatedAt(section.getUpdatedAt());
        
        return dto;
    }

    public static PlatformLandingPageSection toEntity(PlatformSectionRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return PlatformLandingPageSection.builder()
                .sectionType(dto.getSectionType())
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .content(dto.getContent())
                .layoutType(dto.getLayoutType())
                .displayOrder(dto.getDisplayOrder())
                .isVisible(dto.getIsVisible() != null ? dto.getIsVisible() : true)
                .backgroundColor(dto.getBackgroundColor())
                .textColor(dto.getTextColor())
                .imageUrl(dto.getImageUrl())
                .imageKey(dto.getImageKey())
                .metadata(dto.getMetadata())
                .build();
    }
}

