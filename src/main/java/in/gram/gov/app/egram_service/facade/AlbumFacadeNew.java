package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Album;
import in.gram.gov.app.egram_service.domain.entity.GalleryImage;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.dto.filters.AlbumFilter;
import in.gram.gov.app.egram_service.dto.request.AlbumRequestDTO;
import in.gram.gov.app.egram_service.dto.response.AlbumResponseDTO;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import in.gram.gov.app.egram_service.service.AlbumService;
import in.gram.gov.app.egram_service.service.CloudStorageService;
import in.gram.gov.app.egram_service.service.GalleryImageService;
import in.gram.gov.app.egram_service.service.ImageCompressionService;
import in.gram.gov.app.egram_service.service.PanchayatService;
import in.gram.gov.app.egram_service.transformer.AlbumTransformer;
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
import java.util.List;

/**
 * Album Facade
 * Handles album business logic including image processing for cover images
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlbumFacadeNew {
    private final AlbumService albumService;
    private final PanchayatService panchayatService;
    private final ImageCompressionService imageCompressionService;
    private final CloudStorageService cloudStorageService;
    private final GalleryImageService galleryImageService;

    /**
     * Create album with optional cover image upload
     * If cover image provided: compress, upload to B2, and store URL in DB
     */
    @Transactional
    public AlbumResponseDTO create(AlbumRequestDTO request) {
        Long tenantId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(tenantId);

        // Handle cover image upload if provided
        String coverImageUrl = request.getCoverImageUrl();
        if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
            CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
            coverImageUrl = processAndUploadImage(request.getCoverImageFile(), compressionQuality);
        }

        Album album = AlbumTransformer.toEntity(request);
        album.setCoverImageUrl(coverImageUrl);
        album.setPanchayat(panchayat);

        album = albumService.create(album);
        log.info("Album created successfully with ID: {}", album.getId());
        return AlbumTransformer.toDTO(album);
    }

    public AlbumResponseDTO getById(Long id) {
        Album album = albumService.findById(id);
        return AlbumTransformer.toDTO(album);
    }

    public Page<AlbumResponseDTO> getAll(Integer page, Integer size) {
        Long tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Album> albums = albumService.findByPanchayatId(tenantId, pageable);
        return albums.map(AlbumTransformer::toDTO);
    }

    public Page<AlbumResponseDTO> getAll(AlbumFilter albumFilter) {
        Page<Album> albums = albumService.findAll(albumFilter);
        return albums.map(AlbumTransformer::toDTO);
    }


    /**
     * Update album with optional new cover image
     * If new cover image provided: compress, upload to B2, delete old image, and update URL
     */
    @Transactional
    public AlbumResponseDTO update(Long id, AlbumRequestDTO request) {
        Album album = albumService.findById(id);

        if (request.getAlbumName() != null) {
            album.setAlbumName(request.getAlbumName());
        }
        if (request.getDescription() != null) {
            album.setDescription(request.getDescription());
        }

        // Handle cover image update if new image provided
        if (request.getCoverImageFile() != null && !request.getCoverImageFile().isEmpty()) {
            // Delete old cover image if exists
            if (album.getCoverImageUrl() != null && cloudStorageService.isEnabled()) {
                deleteImageFile(album.getCoverImageUrl());
            }
            // Upload new cover image
            CompressionQuality compressionQuality = parseCompressionQuality(request.getCompressionQuality());
            String newCoverImageUrl = processAndUploadImage(request.getCoverImageFile(), compressionQuality);
            album.setCoverImageUrl(newCoverImageUrl);
        } else if (request.getCoverImageUrl() != null) {
            // Update with provided URL if no new file
            album.setCoverImageUrl(request.getCoverImageUrl());
        }

        album = albumService.update(album);
        log.info("Album updated successfully with ID: {}", id);
        return AlbumTransformer.toDTO(album);
    }

    /**
     * Refresh/regenerate presigned URL for album cover image when expired
     * Extracts the file key from current URL and generates a new presigned URL
     *
     * @param id Album ID
     * @return Updated album with fresh presigned URL
     */
    @Transactional
    public AlbumResponseDTO refreshCoverImageUrl(Long id) {
        Album album = albumService.findById(id);

        if (album.getCoverImageUrl() == null || album.getCoverImageUrl().isEmpty()) {
            throw new IllegalArgumentException("Album has no cover image to refresh");
        }

        if (!cloudStorageService.isEnabled()) {
            throw new IllegalStateException("Cloud storage is not enabled");
        }

        try {
            // Extract file key from URL
            String fileKey = extractFileKeyFromUrl(album.getCoverImageUrl());

            if (fileKey == null || fileKey.isEmpty()) {
                throw new IllegalArgumentException("Cannot extract file key from URL: " + album.getCoverImageUrl());
            }

            log.info("Refreshing cover image URL for album ID: {}, file key: {}", id, fileKey);

            // Generate new presigned URL
            ImageCompressionDTO refreshed = cloudStorageService.regeneratePresignedUrl(fileKey);

            if (refreshed != null && refreshed.getBackblazeFileUrl() != null) {
                album.setCoverImageUrl(refreshed.getBackblazeFileUrl());
                album = albumService.update(album);
                log.info("Cover image URL refreshed successfully for album ID: {}", id);
            } else {
                throw new RuntimeException("Failed to generate new presigned URL");
            }

            return AlbumTransformer.toDTO(album);

        } catch (Exception e) {
            log.error("Error refreshing cover image URL for album ID: {}", id, e);
            throw new RuntimeException("Failed to refresh cover image URL: " + e.getMessage(), e);
        }
    }

    /**
     * Delete album and associated cover image from cloud storage
     */
    @Transactional
    public void delete(Long id) {
        Album album = albumService.findById(id);

        // Delete cover image from cloud storage if exists
        if (album.getCoverImageUrl() != null && cloudStorageService.isEnabled()) {
            deleteImageFile(album.getCoverImageUrl());
        }

        albumService.delete(id);
        log.info("Album deleted successfully with ID: {}", id);
    }

    /**
     * Add multiple gallery images to an album
     * Associates existing gallery images with the specified album
     *
     * @param albumId         Album ID
     * @param galleryImageIds List of gallery image IDs to add
     * @return Count of successfully added images
     */
    @Transactional
    public int addImagesToAlbum(Long albumId, List<Long> galleryImageIds) {
        Album album = albumService.findById(albumId);
        int addedCount = 0;

        for (Long imageId : galleryImageIds) {
            try {
                GalleryImage image = galleryImageService.findById(imageId);

                // Only add if not already in this album
                if (image.getAlbum() == null || !image.getAlbum().getId().equals(albumId)) {
                    image.setAlbum(album);
                    galleryImageService.update(image);
                    addedCount++;
                    log.info("Gallery image ID: {} added to album ID: {}", imageId, albumId);
                }
            } catch (Exception e) {
                log.warn("Failed to add gallery image ID: {} to album ID: {}", imageId, albumId, e);
                // Continue with next image instead of failing
            }
        }

        log.info("Successfully added {} out of {} gallery images to album ID: {}",
                addedCount, galleryImageIds.size(), albumId);
        return addedCount;
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

            log.info("Processing cover image for upload: {}", imageFile.getOriginalFilename());

            // Step 1: Validate image
            imageCompressionService.validateImageFile(imageFile);

            // Step 2: Compress image
            ImageCompressionDTO compressionMetadata = imageCompressionService.compressImage(
                    imageFile,
                    compressionQuality
            );

            log.info("Cover image compressed successfully. Original: {} bytes, Compressed: {} bytes",
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

            log.info("Cover image uploaded to cloud storage. URL: {}", uploadResult.getBackblazeFileUrl());
            return uploadResult.getBackblazeFileUrl();

        } catch (Exception e) {
            log.error("Error processing cover image for upload", e);
            // Don't fail the entire operation if image upload fails
            // Return null and the album will be created without cover image
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
                log.info("Deleting cover image from cloud storage. File Key: {}", fileKey);
                cloudStorageService.deleteImage(fileKey);
                log.info("Cover image deleted successfully from cloud storage");
            }
        } catch (Exception e) {
            log.warn("Failed to delete cover image from cloud storage. URL: {}", imageUrl, e);
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
}

