package in.gram.gov.app.egram_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VisibilityUpdateRequestDTO {
    @NotNull(message = "Visibility flag is required")
    private Boolean isVisible;
}

