package in.gram.gov.app.egram_service.dto.filters;

import in.gram.gov.app.egram_service.utility.PaginationParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class AlbumFilter extends PaginationParams {
    private String panchayatSlug;
    private String albumId;
    private String panchayatId;

}
