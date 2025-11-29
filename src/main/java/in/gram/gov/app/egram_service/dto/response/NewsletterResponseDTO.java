package in.gram.gov.app.egram_service.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class NewsletterResponseDTO {
    private Long newsletterId;
    private String title;
    private String subtitle;
    private String coverImageFileKey;
    private String coverImageUrl; // Presigned URL
    private String content;
    private List<String> bulletPoints = new ArrayList<>();
    private LocalDate publishedOn;
    private String authorName;
    private List<String> attachments = new ArrayList<>();
    private Boolean isPublished;
    private Long panchayatId;
    private String panchayatName;
    private Long createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

