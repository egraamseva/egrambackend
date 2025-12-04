package in.gram.gov.app.egram_service.transformer;

import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.PanchayatWebsiteSection;
import in.gram.gov.app.egram_service.dto.request.PanchayatWebsiteSectionRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteSectionResponseDTO;

public class PanchayatWebsiteSectionTransformer {

    public static PanchayatWebsiteSectionResponseDTO toDTO(PanchayatWebsiteSection section) {
        if (section == null) {
            return null;
        }
        
        PanchayatWebsiteSectionResponseDTO dto = new PanchayatWebsiteSectionResponseDTO();
        dto.setId(section.getId());
        if (section.getPanchayat() != null) {
            dto.setPanchayatId(section.getPanchayat().getId());
        }
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

    public static PanchayatWebsiteSection toEntity(PanchayatWebsiteSectionRequestDTO dto, Panchayat panchayat) {
        if (dto == null) {
            return null;
        }
        
        return PanchayatWebsiteSection.builder()
                .panchayat(panchayat)
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

