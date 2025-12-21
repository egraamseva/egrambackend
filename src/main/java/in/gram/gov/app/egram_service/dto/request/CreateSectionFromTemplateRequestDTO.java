package in.gram.gov.app.egram_service.dto.request;

import lombok.Data;

@Data
public class CreateSectionFromTemplateRequestDTO {
    private Integer displayOrder;
    private Boolean isVisible = true;
}

