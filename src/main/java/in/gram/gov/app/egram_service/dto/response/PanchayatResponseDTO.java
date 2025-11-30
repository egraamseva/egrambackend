package in.gram.gov.app.egram_service.dto.response;

import in.gram.gov.app.egram_service.constants.enums.PanchayatStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PanchayatResponseDTO {
    private Long panchayatId;
    private String panchayatName;
    private String slug;
    private String district;
    private String state;
    private String address;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String heroImageUrl;
    private String description;
    private String aboutText;
    private String heroTitle;
    private String heroSubtitle;
    private String aboutTitle;
    private String aboutFeatures; // JSON array stored as string
    private String officeAddress;
    private String officePhone;
    private String officeEmail;
    private String mapCoordinates;
    private String officeHours;
    private Long population;
    private String area;
    private Integer wards;
    private Integer establishedYear;
    private PanchayatStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

