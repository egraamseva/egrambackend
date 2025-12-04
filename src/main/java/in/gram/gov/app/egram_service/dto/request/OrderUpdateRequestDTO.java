package in.gram.gov.app.egram_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderUpdateRequestDTO {
    @NotNull(message = "Display order is required")
    private Integer displayOrder;
}

