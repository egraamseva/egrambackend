package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Album;
import in.gram.gov.app.egram_service.domain.entity.GalleryImage;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.dto.filters.GalleryFilter;
import in.gram.gov.app.egram_service.dto.request.GalleryImageRequestDTO;
import in.gram.gov.app.egram_service.dto.response.GalleryImageResponseDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.service.*;
import in.gram.gov.app.egram_service.transformer.GalleryImageTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GalleryImageFacade {
    private final GalleryImageService galleryImageService;
    private final PanchayatService panchayatService;
    private final AlbumService albumService;
    private final UserService userService;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;

    /**
     * Create gallery image with optional image file upload
     * If image is provided: compress it, upload to B2, and store URL in DB
     */
    @Transactional
    public GalleryImageResponseDTO create(GalleryImageRequestDTO request, String email) {
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);
        User uploader = userService.findByEmail(email);

        // Handle image upload if provided
        String imageUrl = request.getImageUrl();
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
            imageUrl = processAndUploadImage(request.getImageFile(), compressionQuality);
        }

        GalleryImage image = GalleryImageTransformer.toEntity(request);
        image.setImageUrl(imageUrl);
        image.setPanchayat(panchayat);
        image.setUploadedBy(uploader);

        if (request.getAlbumId() != null) {
            Album album = albumService.findById(request.getAlbumId());
            image.setAlbum(album);
        }

        image = galleryImageService.create(image);
        log.info("Gallery image created successfully with ID: {}", image.getId());
        return GalleryImageTransformer.toDTO(image);
    }

    public GalleryImageResponseDTO getById(Long id) {
        GalleryImage image = galleryImageService.findById(id);
        return GalleryImageTransformer.toDTO(image);
    }

    public Page<GalleryImageResponseDTO> getAll(GalleryFilter galleryFilter) {
        Long tenantId = TenantContext.getTenantId();

        galleryFilter.setPanchayatId(tenantId);

        Page<GalleryImage> images = galleryImageService.findAll(galleryFilter);
        return images.map(GalleryImageTransformer::toDTO);
    }


    /**
     * Update gallery image with optional new image file
     * If new image provided: compress, upload to B2, delete old image, and update URL
     */
    @Transactional
    public GalleryImageResponseDTO update(Long id, GalleryImageRequestDTO request) {
        GalleryImage image = galleryImageService.findById(id);

        if (request.getCaption() != null) {
            image.setCaption(request.getCaption());
        }
        if (request.getTags() != null) {
            image.setTags(request.getTags());
        }
        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getAlbumId() != null) {
            Album album = albumService.findById(request.getAlbumId());
            image.setAlbum(album);
        } else if (request.getAlbumId() == null && image.getAlbum() != null) {
            // Allow removing from album by passing null
            image.setAlbum(null);
        }

        // Handle image update if new image provided
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            // Delete old image if exists
            if (image.getImageUrl() != null && cloudStorageService.isEnabled()) {
                deleteImageFile(image.getImageUrl());
            }
            // Upload new image
            CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
            String newImageUrl = processAndUploadImage(request.getImageFile(), compressionQuality);
            image.setImageUrl(newImageUrl);
        } else if (request.getImageUrl() != null) {
            // Update with provided URL if no new file
            image.setImageUrl(request.getImageUrl());
        }

        image = galleryImageService.update(image);
        log.info("Gallery image updated successfully with ID: {}", id);
        return GalleryImageTransformer.toDTO(image);
    }

    /**
     * Refresh/regenerate presigned URL for gallery image when expired
     * Extracts the file key from current URL and generates a new presigned URL
     *
     * @param id Gallery image ID
     * @return Updated gallery image with fresh presigned URL
     */
    @Transactional
    public GalleryImageResponseDTO refreshImageUrl(Long id) {
        GalleryImage image = galleryImageService.findById(id);

        if (image.getImageUrl() == null || image.getImageUrl().isEmpty()) {
            throw new IllegalArgumentException("Gallery image has no image to refresh");
        }

        if (!cloudStorageService.isEnabled()) {
            throw new IllegalStateException("Cloud storage is not enabled");
        }

        try {
            // Extract file key from URL
            String fileKey = extractFileKeyFromUrl(image.getImageUrl());

            if (fileKey == null || fileKey.isEmpty()) {
                throw new IllegalArgumentException("Cannot extract file key from URL: " + image.getImageUrl());
            }

            log.info("Refreshing image URL for gallery image ID: {}, file key: {}", id, fileKey);

            // Generate new presigned URL
            ImageCompressionDTO refreshed = cloudStorageService.regeneratePresignedUrl(fileKey);

            if (refreshed != null && refreshed.getBackblazeFileUrl() != null) {
                image.setImageUrl(refreshed.getBackblazeFileUrl());
                image = galleryImageService.update(image);
                log.info("Image URL refreshed successfully for gallery image ID: {}", id);
            } else {
                throw new RuntimeException("Failed to generate new presigned URL");
            }

            return GalleryImageTransformer.toDTO(image);

        } catch (Exception e) {
            log.error("Error refreshing image URL for gallery image ID: {}", id, e);
            throw new RuntimeException("Failed to refresh image URL: " + e.getMessage(), e);
        }
    }

    /**
     * Delete gallery image and associated image from cloud storage
     */
    @Transactional
    public void delete(Long id) {
        GalleryImage image = galleryImageService.findById(id);

        // Delete image from cloud storage if exists
        if (image.getImageUrl() != null && cloudStorageService.isEnabled()) {
            deleteImageFile(image.getImageUrl());
        }

        galleryImageService.delete(id);
        log.info("Gallery image deleted successfully with ID: {}", id);
    }

    /**
     * Process image: compress and upload to Backblaze B2
     *
     * @param imageFile          MultipartFile to process
     * @param compressionQuality Quality level for compression
     * @return URL of uploaded image or null if storage is disabled
     */
    private String processAndUploadImage(MultipartFile imageFile, CompressionQuality compressionQuality) {
        try {
            if (!cloudStorageService.isEnabled()) {
                log.warn("Cloud storage is disabled. Image upload skipped");
                return null;
            }

            if (compressionQuality == null) {
                compressionQuality = CompressionQuality.HIGH;
            }

            log.info("Processing image for upload: {}", imageFile.getOriginalFilename());

            // Step 1: Validate image
            imageCompressionService.validateImageFile(imageFile);

            // Step 2: Compress image
            ImageCompressionDTO compressionMetadata = imageCompressionService.compressImage(
                    imageFile,
                    compressionQuality
            );

            log.info("Image compressed successfully. Original: {} bytes, Compressed: {} bytes",
                    compressionMetadata.getOriginalFileSize(),
                    compressionMetadata.getCompressedFileSize());

            // Step 3: Get compressed image stream
            InputStream compressedImageStream = imageCompressionService.getCompressedImageInputStream(
                    imageFile,
                    compressionQuality
            );

            // Step 4: Upload to Backblaze B2
            ImageCompressionDTO uploadResult = cloudStorageService.uploadImage(
                    compressedImageStream,
                    compressionMetadata
            );

            log.info("Image uploaded to cloud storage. URL: {}", uploadResult.getBackblazeFileUrl());
            return uploadResult.getBackblazeFileUrl();

        } catch (Exception e) {
            log.error("Error processing image for upload", e);
            // Don't fail the entire operation if image upload fails
            // Return null and the gallery image will be created without image
            return null;
        }
    }

    /**
     * Extract file key from Backblaze B2 presigned URL
     * URL format: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext
     * File key format: images/timestamp-uuid.ext
     *
     * @param imageUrl The presigned URL
     * @return Extracted file key or null if invalid URL format
     */
    private String extractFileKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            int fileIndex = imageUrl.indexOf("/file/");
            if (fileIndex == -1) {
                log.warn("Invalid image URL format - '/file/' not found: {}", imageUrl);
                return null;
            }

            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = imageUrl.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                log.warn("Cannot extract bucket name from URL: {}", imageUrl);
                return null;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = imageUrl.substring(bucketEnd + 1);

            if (fileKey.isEmpty()) {
                log.warn("Extracted file key is empty from URL: {}", imageUrl);
                return null;
            }

            // Remove query parameters if present (from presigned URLs)
            int queryIndex = fileKey.indexOf("?");
            if (queryIndex != -1) {
                fileKey = fileKey.substring(0, queryIndex);
            }

            return fileKey;

        } catch (Exception e) {
            log.error("Error extracting file key from URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * Delete image from cloud storage
     * Extracts S3 key (file path) from the stored URL
     * File URL format: https://f001.backblazeb2.com/file/bucket-name/images/timestamp-uuid.ext
     * File key format: images/timestamp-uuid.ext
     */
    private void deleteImageFile(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            // Extract file key from URL
            int fileIndex = imageUrl.indexOf("/file/");
            if (fileIndex == -1) {
                log.warn("Invalid image URL format: {}", imageUrl);
                return;
            }

            int bucketStart = fileIndex + 6; // "/file/" length
            int bucketEnd = imageUrl.indexOf("/", bucketStart);
            if (bucketEnd == -1) {
                log.warn("Cannot extract bucket name from URL: {}", imageUrl);
                return;
            }

            // Extract file key (everything after bucket-name/)
            String fileKey = imageUrl.substring(bucketEnd + 1);

            // Remove query parameters if present (from presigned URLs)
            int queryIndex = fileKey.indexOf("?");
            if (queryIndex != -1) {
                fileKey = fileKey.substring(0, queryIndex);
            }

            if (!fileKey.isEmpty()) {
                log.info("Deleting image from cloud storage. File Key: {}", fileKey);
                cloudStorageService.deleteImage(fileKey);
                log.info("Image deleted successfully from cloud storage");
            }
        } catch (Exception e) {
            log.warn("Failed to delete image from cloud storage. URL: {}", imageUrl, e);
            // Don't throw exception - log and continue
        }
    }

    /**
     * Parse compression quality string to enum
     */
    private CompressionQuality parseCompressionQuality(String qualityString) {
        if (qualityString == null || qualityString.isEmpty()) {
            return CompressionQuality.HIGH;
        }

        try {
            return CompressionQuality.valueOf(qualityString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid compression quality: {}, using HIGH", qualityString);
            return CompressionQuality.HIGH;
        }
    }

    public Page<GalleryImageResponseDTO> getBySlug(String slug, Integer page, Integer size, Long albumId) {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                Sort.by(Sort.Direction.ASC, "displayOrder")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Page<GalleryImage> images;
        if (albumId != null) {
            images = galleryImageService.findByPanchayatSlugAndAlbumId(slug, albumId, pageable);
        } else {
            // Need to add a method to find by slug without album
            // For now, we'll get panchayat by slug first
            Panchayat panchayat = panchayatService.findBySlug(slug);
            images = galleryImageService.findByPanchayatId(panchayat.getId(), pageable);
        }

        return images.map(GalleryImageTransformer::toDTO);
    }
}

