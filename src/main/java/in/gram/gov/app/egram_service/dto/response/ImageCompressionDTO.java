package in.gram.gov.app.egram_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to hold image compression metadata and cloud storage information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageCompressionDTO {
    private String originalFileName;
    private Long originalFileSize; // in bytes
    private Long compressedFileSize; // in bytes
    private Float compressionRatio; // percentage
    private String contentType;
    private Integer width;
    private Integer height;
    private String backblazeFileId; // S3 key/path in Backblaze B2
    private String backblazeFileUrl; // Public URL to access the image
}

