package in.gram.gov.app.egram_service.transformer;

import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.dto.request.PanchayatRequestDTO;
import in.gram.gov.app.egram_service.dto.response.PanchayatResponseDTO;

public class PanchayatTransformer {

    public static PanchayatResponseDTO toDTO(Panchayat panchayat) {
        if (panchayat == null) {
            return null;
        }
        
        PanchayatResponseDTO dto = new PanchayatResponseDTO();
        dto.setPanchayatId(panchayat.getId());
        dto.setPanchayatName(panchayat.getPanchayatName());
        dto.setSlug(panchayat.getSlug());
        dto.setDistrict(panchayat.getDistrict());
        dto.setState(panchayat.getState());
        dto.setAddress(panchayat.getAddress());
        dto.setContactPhone(panchayat.getContactPhone());
        dto.setContactEmail(panchayat.getContactEmail());
        dto.setLogoUrl(panchayat.getLogoUrl());
        dto.setHeroImageUrl(panchayat.getHeroImageUrl());
        dto.setDescription(panchayat.getDescription());
        dto.setAboutText(panchayat.getAboutText());
        dto.setHeroTitle(panchayat.getHeroTitle());
        dto.setHeroSubtitle(panchayat.getHeroSubtitle());
        dto.setAboutTitle(panchayat.getAboutTitle());
        dto.setAboutFeatures(panchayat.getAboutFeatures());
        dto.setOfficeAddress(panchayat.getOfficeAddress());
        dto.setOfficePhone(panchayat.getOfficePhone());
        dto.setOfficeEmail(panchayat.getOfficeEmail());
        dto.setMapCoordinates(panchayat.getMapCoordinates());
        dto.setOfficeHours(panchayat.getOfficeHours());
        dto.setPopulation(panchayat.getPopulation());
        dto.setArea(panchayat.getArea());
        dto.setWards(panchayat.getWards());
        dto.setEstablishedYear(panchayat.getEstablishedYear());
        dto.setThemeId(panchayat.getThemeId());
        dto.setStatus(panchayat.getStatus());
        dto.setCreatedAt(panchayat.getCreatedAt());
        dto.setUpdatedAt(panchayat.getUpdatedAt());
        
        return dto;
    }

    public static Panchayat toEntity(PanchayatRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Panchayat.builder()
                .panchayatName(dto.getPanchayatName())
                .slug(dto.getSlug())
                .district(dto.getDistrict())
                .state(dto.getState())
                .address(dto.getAddress())
                .contactPhone(dto.getContactPhone())
                .contactEmail(dto.getContactEmail())
                .description(dto.getDescription())
                .aboutText(dto.getAboutText())
                .heroTitle(dto.getHeroTitle())
                .heroSubtitle(dto.getHeroSubtitle())
                .aboutTitle(dto.getAboutTitle())
                .aboutFeatures(dto.getAboutFeatures())
                .officeAddress(dto.getOfficeAddress())
                .officePhone(dto.getOfficePhone())
                .officeEmail(dto.getOfficeEmail())
                .mapCoordinates(dto.getMapCoordinates())
                .officeHours(dto.getOfficeHours())
                .population(dto.getPopulation())
                .area(dto.getArea())
                .wards(dto.getWards())
                .establishedYear(dto.getEstablishedYear())
                .logoUrl(dto.getLogoUrl())
                .heroImageUrl(dto.getHeroImageUrl())
                .themeId(dto.getThemeId())
                .build();
    }

    public static void updateEntity(Panchayat panchayat, PanchayatRequestDTO dto) {
        if (panchayat == null || dto == null) {
            return;
        }
        
        // Only update fields that are provided (not null)
        if (dto.getPanchayatName() != null) {
            panchayat.setPanchayatName(dto.getPanchayatName());
        }
        if (dto.getSlug() != null) {
            panchayat.setSlug(dto.getSlug());
        }
        if (dto.getDistrict() != null) {
            panchayat.setDistrict(dto.getDistrict());
        }
        if (dto.getState() != null) {
            panchayat.setState(dto.getState());
        }
        if (dto.getAddress() != null) {
            panchayat.setAddress(dto.getAddress());
        }
        if (dto.getContactPhone() != null) {
            panchayat.setContactPhone(dto.getContactPhone());
        }
        if (dto.getContactEmail() != null) {
            panchayat.setContactEmail(dto.getContactEmail());
        }
        if (dto.getDescription() != null) {
            panchayat.setDescription(dto.getDescription());
        }
        if (dto.getAboutText() != null) {
            panchayat.setAboutText(dto.getAboutText());
        }
        if (dto.getHeroTitle() != null) {
            panchayat.setHeroTitle(dto.getHeroTitle());
        }
        if (dto.getHeroSubtitle() != null) {
            panchayat.setHeroSubtitle(dto.getHeroSubtitle());
        }
        if (dto.getAboutTitle() != null) {
            panchayat.setAboutTitle(dto.getAboutTitle());
        }
        if (dto.getAboutFeatures() != null) {
            panchayat.setAboutFeatures(dto.getAboutFeatures());
        }
        if (dto.getOfficeAddress() != null) {
            panchayat.setOfficeAddress(dto.getOfficeAddress());
        }
        if (dto.getOfficePhone() != null) {
            panchayat.setOfficePhone(dto.getOfficePhone());
        }
        if (dto.getOfficeEmail() != null) {
            panchayat.setOfficeEmail(dto.getOfficeEmail());
        }
        if (dto.getMapCoordinates() != null) {
            panchayat.setMapCoordinates(dto.getMapCoordinates());
        }
        if (dto.getOfficeHours() != null) {
            panchayat.setOfficeHours(dto.getOfficeHours());
        }
        if (dto.getPopulation() != null) {
            panchayat.setPopulation(dto.getPopulation());
        }
        if (dto.getArea() != null) {
            panchayat.setArea(dto.getArea());
        }
        if (dto.getWards() != null) {
            panchayat.setWards(dto.getWards());
        }
        if (dto.getEstablishedYear() != null) {
            panchayat.setEstablishedYear(dto.getEstablishedYear());
        }
        if (dto.getLogoUrl() != null) {
            panchayat.setLogoUrl(dto.getLogoUrl());
        }
        if (dto.getHeroImageUrl() != null) {
            panchayat.setHeroImageUrl(dto.getHeroImageUrl());
        }
        if (dto.getThemeId() != null) {
            panchayat.setThemeId(dto.getThemeId());
        }
    }
}

