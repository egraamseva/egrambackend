package in.gram.gov.app.egram_service.transformer;

import in.gram.gov.app.egram_service.domain.entity.Newsletter;
import in.gram.gov.app.egram_service.dto.request.NewsletterRequestDTO;
import in.gram.gov.app.egram_service.dto.response.NewsletterResponseDTO;
import in.gram.gov.app.egram_service.service.S3CloudStorageService;

import java.util.ArrayList;

public class NewsletterTransformer {

    public static NewsletterResponseDTO toDTO(Newsletter newsletter, S3CloudStorageService s3CloudStorageService) {
        if (newsletter == null) {
            return null;
        }
        
        NewsletterResponseDTO dto = new NewsletterResponseDTO();
        dto.setNewsletterId(newsletter.getId());
        dto.setTitle(newsletter.getTitle());
        dto.setSubtitle(newsletter.getSubtitle());
        dto.setCoverImageFileKey(newsletter.getCoverImageFileKey());
        
        // Generate presigned URL for cover image if file key exists
        if (newsletter.getCoverImageFileKey() != null && !newsletter.getCoverImageFileKey().isEmpty() && s3CloudStorageService != null) {
            try {
                String presignedUrl = s3CloudStorageService.getFileUrl(newsletter.getCoverImageFileKey());
                dto.setCoverImageUrl(presignedUrl);
            } catch (Exception e) {
                // Log but don't fail - cover image URL generation is optional
                dto.setCoverImageUrl(null);
            }
        }
        
        dto.setContent(newsletter.getContent());
        dto.setBulletPoints(newsletter.getBulletPoints() != null ? new ArrayList<>(newsletter.getBulletPoints()) : new ArrayList<>());
        dto.setPublishedOn(newsletter.getPublishedOn());
        dto.setAuthorName(newsletter.getAuthorName());
        dto.setAttachments(newsletter.getAttachments() != null ? new ArrayList<>(newsletter.getAttachments()) : new ArrayList<>());
        dto.setIsPublished(newsletter.getIsPublished());
        dto.setCreatedAt(newsletter.getCreatedAt());
        dto.setUpdatedAt(newsletter.getUpdatedAt());
        
        if (newsletter.getPanchayat() != null) {
            dto.setPanchayatId(newsletter.getPanchayat().getId());
            dto.setPanchayatName(newsletter.getPanchayat().getPanchayatName());
        }
        
        if (newsletter.getCreatedBy() != null) {
            dto.setCreatedByUserId(newsletter.getCreatedBy().getId());
            dto.setCreatedByName(newsletter.getCreatedBy().getName());
        }
        
        return dto;
    }

    public static Newsletter toEntity(NewsletterRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Newsletter.builder()
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .coverImageFileKey(dto.getCoverImageFileKey())
                .content(dto.getContent())
                .bulletPoints(dto.getBulletPoints() != null ? new ArrayList<>(dto.getBulletPoints()) : new ArrayList<>())
                .publishedOn(dto.getPublishedOn())
                .authorName(dto.getAuthorName())
                .attachments(dto.getAttachments() != null ? new ArrayList<>(dto.getAttachments()) : new ArrayList<>())
                .isPublished(dto.getIsPublished() != null ? dto.getIsPublished() : false)
                .build();
    }
}

