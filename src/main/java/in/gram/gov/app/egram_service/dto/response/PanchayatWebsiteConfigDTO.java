package in.gram.gov.app.egram_service.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PanchayatWebsiteConfigDTO {
    private List<PanchayatWebsiteSectionResponseDTO> sections;
}

