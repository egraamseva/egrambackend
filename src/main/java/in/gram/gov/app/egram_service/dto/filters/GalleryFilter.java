package in.gram.gov.app.egram_service.dto.filters;

import in.gram.gov.app.egram_service.utility.PaginationParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class GalleryFilter extends PaginationParams {
    private Long albumId;
    private Long panchayatId;
}
