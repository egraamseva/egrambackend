package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.enums.CompressionQuality;
import in.gram.gov.app.egram_service.constants.exception.ImageUploadException;
import in.gram.gov.app.egram_service.dto.response.ImageCompressionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for handling image compression and optimization
 * Compresses images before upload to reduce storage space and bandwidth
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageCompressionService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(
            Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp")
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp")
    );

    @Value("${cloud.storage.image.max-width:2560}")
    private int maxWidth;

    @Value("${cloud.storage.image.max-height:2560}")
    private int maxHeight;

    @Value("${cloud.storage.image.max-size-bytes:10485760}") // 10MB default
    private long maxFileSizeBytes;

    @Value("${cloud.storage.image.compression-quality:HIGH}")
    private String defaultCompressionQuality;

    @Value("${cloud.storage.image.target-size-bytes:256000}") // 500KB target
    private long targetFileSizeBytes;

    /**
     * Validate image file
     * @param file MultipartFile to validate
     * @throws ImageUploadException if validation fails
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("Image file is required");
        }

        // Check file size
        if (file.getSize() > maxFileSizeBytes) {
            throw new ImageUploadException(
                    "Image file size exceeds maximum limit of " + (maxFileSizeBytes / (1024 * 1024)) + "MB"
            );
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ImageUploadException(
                    "Invalid image format. Allowed formats: JPEG, PNG, GIF, WebP"
            );
        }

        // Check file extension matches content type
        String filename = file.getOriginalFilename();
        if (filename == null || !isValidImageExtension(filename)) {
            throw new ImageUploadException("Invalid image file extension");
        }

        log.info("Image validation passed for file: {}", filename);
    }

    /**
     * Compress image and return compressed file metadata
     * Uses iterative compression to ensure file size is under 500KB target
     * @param file Original image file
     * @param quality Compression quality level
     * @return ImageCompressionDTO with compressed data and metadata
     */
    public ImageCompressionDTO compressImage(MultipartFile file, CompressionQuality quality) {
        try {
            validateImageFile(file);

            String originalFileName = file.getOriginalFilename();
            long originalFileSize = file.getSize();
            String contentType = file.getContentType();

            log.info("Starting compression for file: {}, size: {} bytes, quality: {}",
                    originalFileName, originalFileSize, quality.getDescription());

            // Read file bytes with error handling
            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
                if (fileBytes == null || fileBytes.length == 0) {
                    throw new ImageUploadException("File bytes are null or empty");
                }
                log.info("Successfully read {} bytes from file: {}", fileBytes.length, originalFileName);
                
                // Log first few bytes for debugging
                if (fileBytes.length >= 4) {
                    String hexBytes = String.format("%02X %02X %02X %02X", 
                            fileBytes[0] & 0xFF, fileBytes[1] & 0xFF, 
                            fileBytes[2] & 0xFF, fileBytes[3] & 0xFF);
                    log.info("First 4 bytes (hex): {}", hexBytes);
                }
                
                // Check if file might be base64 encoded (starts with data: or is all printable ASCII)
                if (fileBytes.length > 0 && (fileBytes[0] == 'd' || fileBytes[0] == 'D')) {
                    String firstChars = new String(fileBytes, 0, Math.min(20, fileBytes.length));
                    if (firstChars.startsWith("data:image/")) {
                        log.error("File appears to be base64 encoded data URI. Expected raw image bytes.");
                        throw new ImageUploadException("Image file appears to be base64 encoded. Please send raw image bytes.");
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read bytes from file: {}", originalFileName, e);
                throw new ImageUploadException("Failed to read file bytes: " + e.getMessage(), e);
            }

            // Read original image with better error handling
            BufferedImage originalImage;
            try {
                // First, try to verify the file bytes start with valid image magic bytes
                if (fileBytes.length < 4) {
                    throw new ImageUploadException("File is too small to be a valid image");
                }
                
                // Check for common image format magic bytes and detect actual format
                String detectedFormat = null;
                boolean isValidImage = false;
                
                if (fileBytes[0] == (byte)0xFF && fileBytes[1] == (byte)0xD8) {
                    // JPEG
                    detectedFormat = "JPEG";
                    isValidImage = true;
                } else if (fileBytes[0] == (byte)0x89 && fileBytes[1] == (byte)0x50 && 
                          fileBytes[2] == (byte)0x4E && fileBytes[3] == (byte)0x47) {
                    // PNG
                    detectedFormat = "PNG";
                    isValidImage = true;
                } else if (fileBytes[0] == (byte)0x47 && fileBytes[1] == (byte)0x49 && 
                          fileBytes[2] == (byte)0x46) {
                    // GIF
                    detectedFormat = "GIF";
                    isValidImage = true;
                } else if (fileBytes[0] == (byte)0x52 && fileBytes[1] == (byte)0x49 && 
                          fileBytes[2] == (byte)0x46 && fileBytes[3] == (byte)0x46) {
                    // RIFF header - could be WebP or other RIFF format
                    if (fileBytes.length > 11 &&
                        fileBytes[8] == (byte)0x57 && fileBytes[9] == (byte)0x45 && 
                        fileBytes[10] == (byte)0x42 && fileBytes[11] == (byte)0x50) {
                        // WebP
                        detectedFormat = "WebP";
                        isValidImage = true;
                    } else {
                        // RIFF but not WebP - might be incomplete or corrupted
                        detectedFormat = "RIFF (not WebP)";
                        log.warn("File has RIFF header but missing WebP signature. File: {}, Size: {} bytes", 
                                originalFileName, fileBytes.length);
                    }
                }
                
                // Check for format mismatch between content type and detected format
                if (detectedFormat != null && contentType != null) {
                    String expectedFormat = contentType.contains("jpeg") || contentType.contains("jpg") ? "JPEG" :
                                           contentType.contains("png") ? "PNG" :
                                           contentType.contains("gif") ? "GIF" :
                                           contentType.contains("webp") ? "WebP" : null;
                    
                    if (expectedFormat != null && !detectedFormat.equals(expectedFormat) && 
                        !detectedFormat.startsWith("RIFF")) {
                        log.warn("Format mismatch detected. File: {}, Content-Type: {} (expects {}), Actual format: {}", 
                                originalFileName, contentType, expectedFormat, detectedFormat);
                    }
                }
                
                if (!isValidImage) {
                    String hexBytes = String.format("%02X %02X %02X %02X", 
                            fileBytes[0] & 0xFF, fileBytes[1] & 0xFF, 
                            fileBytes[2] & 0xFF, fileBytes[3] & 0xFF);
                    String formatInfo = detectedFormat != null ? 
                            String.format(" Detected format: %s", detectedFormat) : "";
                    log.error("File does not appear to be a valid image based on magic bytes. File: {}, First bytes: {}, Content-Type: {}.{}", 
                            originalFileName, hexBytes, contentType, formatInfo);
                    throw new ImageUploadException(
                            String.format("Invalid image format. File '%s' does not have valid image magic bytes. First bytes: %s. Content-Type: %s.%s The file may be corrupted, incomplete, or not a valid image file.", 
                            originalFileName, hexBytes, contentType, formatInfo));
                }
                
                log.info("Detected image format: {} for file: {} (Content-Type: {})", 
                        detectedFormat, originalFileName, contentType);
                
                // Try to read image with Thumbnailator
                try {
                    originalImage = Thumbnails.of(new ByteArrayInputStream(fileBytes))
                            .size(maxWidth, maxHeight)
                            .asBufferedImage();
                } catch (net.coobird.thumbnailator.tasks.UnsupportedFormatException e) {
                    // If Thumbnailator fails, try Java ImageIO as fallback
                    log.warn("Thumbnailator failed to read image, trying Java ImageIO as fallback. File: {}, Detected format: {}", 
                            originalFileName, detectedFormat);
                    try {
                        javax.imageio.ImageIO.setUseCache(false);
                        originalImage = javax.imageio.ImageIO.read(new ByteArrayInputStream(fileBytes));
                        if (originalImage == null) {
                            // Check if it's a format mismatch issue
                            String formatMismatchMsg = "";
                            if (detectedFormat != null && contentType != null) {
                                boolean isMismatch = (detectedFormat.equals("WebP") && contentType.contains("jpeg")) ||
                                                    (detectedFormat.equals("JPEG") && contentType.contains("webp")) ||
                                                    (detectedFormat.startsWith("RIFF") && !contentType.contains("webp"));
                                if (isMismatch) {
                                    formatMismatchMsg = String.format(" Format mismatch: file appears to be %s but Content-Type is %s.", 
                                            detectedFormat, contentType);
                                }
                            }
                            throw new ImageUploadException(
                                    String.format("Java ImageIO failed to read the image. The file may be corrupted, incomplete, or in an unsupported format.%s Please ensure the file is a valid, complete image file.", 
                                    formatMismatchMsg));
                        }
                        // Resize if needed
                        if (originalImage.getWidth() > maxWidth || originalImage.getHeight() > maxHeight) {
                            double scaleX = (double) maxWidth / originalImage.getWidth();
                            double scaleY = (double) maxHeight / originalImage.getHeight();
                            double scale = Math.min(scaleX, scaleY);
                            int newWidth = (int) (originalImage.getWidth() * scale);
                            int newHeight = (int) (originalImage.getHeight() * scale);
                            originalImage = Thumbnails.of(originalImage)
                                    .size(newWidth, newHeight)
                                    .asBufferedImage();
                        }
                        log.info("Successfully read image using Java ImageIO fallback. Dimensions: {}x{}", 
                                originalImage.getWidth(), originalImage.getHeight());
                    } catch (ImageUploadException imgEx) {
                        // Re-throw ImageUploadException as-is
                        throw imgEx;
                    } catch (Exception ioException) {
                        // Check for format mismatch
                        String formatMismatchMsg = "";
                        if (detectedFormat != null && contentType != null) {
                            boolean isMismatch = (detectedFormat.equals("WebP") && contentType.contains("jpeg")) ||
                                                (detectedFormat.equals("JPEG") && contentType.contains("webp")) ||
                                                (detectedFormat.startsWith("RIFF") && !contentType.contains("webp"));
                            if (isMismatch) {
                                formatMismatchMsg = String.format(" CRITICAL: Format mismatch detected - file appears to be %s but Content-Type is %s. The file extension may be incorrect.", 
                                        detectedFormat, contentType);
                            }
                        }
                        log.error("Both Thumbnailator and Java ImageIO failed to read image. File: {}, Size: {} bytes, Content-Type: {}, Detected format: {}, Error: {}", 
                                originalFileName, fileBytes.length, contentType, detectedFormat, ioException.getMessage(), ioException);
                        throw new ImageUploadException(
                                String.format("Failed to read image data. The file '%s' may be corrupted, incomplete, or in an unsupported format.%s Thumbnailator error: %s, ImageIO error: %s. Please ensure the file is a valid, complete image file and the file extension matches the actual format.", 
                                originalFileName, formatMismatchMsg, e.getMessage(), ioException.getMessage()), e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to read image from bytes. File: {}, Size: {} bytes, ContentType: {}, Error: {}", 
                        originalFileName, fileBytes.length, contentType, e.getMessage(), e);
                throw new ImageUploadException("Failed to read image data. The file may be corrupted or in an unsupported format: " + e.getMessage(), e);
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // Determine output format from content type
            String outputFormat = getOutputFormat(contentType);

            // Compress with iterative quality reduction to reach 500KB target
            byte[] compressedImageBytes = compressToTargetSize(originalImage, width, height,
                    outputFormat, quality.getQualityPercentage());

            long compressedFileSize = compressedImageBytes.length;
            float compressionRatio = ((originalFileSize - compressedFileSize) * 100.0f) / originalFileSize;

            log.info("Image compression completed. Original: {} bytes, Compressed: {} bytes, Ratio: {:.2f}%",
                    originalFileSize, compressedFileSize, compressionRatio);

            return ImageCompressionDTO.builder()
                    .originalFileName(originalFileName)
                    .originalFileSize(originalFileSize)
                    .compressedFileSize(compressedFileSize)
                    .compressionRatio(compressionRatio)
                    .contentType(contentType)
                    .width(width)
                    .height(height)
                    .build();

        } catch (IOException e) {
            log.error("Error during image compression", e);
            throw new ImageUploadException("Failed to compress image: " + e.getMessage(), e);
        }
    }

    /**
     * Compress image to target size by iteratively reducing quality
     * Ensures image doesn't exceed 500KB
     * @param image BufferedImage to compress
     * @param width Image width
     * @param height Image height
     * @param outputFormat Output format (jpeg, png, etc)
     * @param initialQuality Starting quality (0.0 to 1.0)
     * @return Compressed image bytes
     */
    private byte[] compressToTargetSize(BufferedImage image, int width, int height,
                                       String outputFormat, float initialQuality) throws IOException {
        float quality = initialQuality;
        float scaleFactor = 1.0f;
        byte[] compressedBytes = null;

        // First attempt: compress with original dimensions and quality
        compressedBytes = compressImage(image, width, height, outputFormat, quality);

        // If still over target size, iteratively reduce quality and/or scale
        int attempts = 0;
        int maxAttempts = 15;

        while (compressedBytes.length > targetFileSizeBytes && attempts < maxAttempts) {
            attempts++;

            // Reduce quality more aggressively for JPEG
            if ("jpeg".equalsIgnoreCase(outputFormat)) {
                quality = Math.max(0.10f, quality - 0.08f); // Reduce by 8% each iteration, min 10%
            } else if ("png".equalsIgnoreCase(outputFormat)) {
                // For PNG, scale down image instead
                scaleFactor *= 0.9f;
                quality = Math.max(0.10f, quality - 0.05f);
            } else if ("webp".equalsIgnoreCase(outputFormat)) {
                quality = Math.max(0.10f, quality - 0.10f);
            } else {
                quality = Math.max(0.10f, quality - 0.08f);
            }

            int newWidth = (int) (width * scaleFactor);
            int newHeight = (int) (height * scaleFactor);

            // Ensure minimum dimensions
            newWidth = Math.max(100, newWidth);
            newHeight = Math.max(100, newHeight);

            compressedBytes = compressImage(image, newWidth, newHeight, outputFormat, quality);

            log.debug("Compression attempt {}: quality={}, scale={}, size={} bytes",
                    attempts, String.format("%.2f", quality), String.format("%.2f", scaleFactor), compressedBytes.length);
        }

        if (compressedBytes.length > targetFileSizeBytes) {
            log.warn("Image compression: Could not reduce below 500KB target. Final size: {} bytes",
                    compressedBytes.length);
        } else {
            log.info("Image successfully compressed to {} bytes (under 500KB target)",
                    compressedBytes.length);
        }

        return compressedBytes;
    }

    /**
     * Compress image with specified parameters
     * @param image BufferedImage to compress
     * @param width Target width
     * @param height Target height
     * @param outputFormat Output format
     * @param quality Quality percentage
     * @return Compressed image bytes
     */
    private byte[] compressImage(BufferedImage image, int width, int height,
                                String outputFormat, float quality) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .size(width, height)
                .outputFormat(outputFormat)
                .outputQuality(quality)
                .toOutputStream(output);
        return output.toByteArray();
    }

    /**
     * Get compressed image as InputStream
     * @param file Original image file
     * @param quality Compression quality level
     * @return InputStream of compressed image
     */
    public InputStream getCompressedImageInputStream(MultipartFile file, CompressionQuality quality) {
        try {
            validateImageFile(file);

            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();

            log.info("Generating compressed image stream for: {}", originalFileName);

            // Read file bytes with error handling
            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
                if (fileBytes == null || fileBytes.length == 0) {
                    throw new ImageUploadException("File bytes are null or empty");
                }
                log.info("Successfully read {} bytes from file for stream generation: {}", fileBytes.length, originalFileName);
            } catch (IOException e) {
                log.error("Failed to read bytes from file: {}", originalFileName, e);
                throw new ImageUploadException("Failed to read file bytes: " + e.getMessage(), e);
            }

            // Read original image with better error handling
            BufferedImage originalImage;
            try {
                // First, try to verify the file bytes start with valid image magic bytes
                if (fileBytes.length < 4) {
                    throw new ImageUploadException("File is too small to be a valid image");
                }
                
                // Check for common image format magic bytes and detect actual format
                String detectedFormat = null;
                boolean isValidImage = false;
                
                if (fileBytes[0] == (byte)0xFF && fileBytes[1] == (byte)0xD8) {
                    // JPEG
                    detectedFormat = "JPEG";
                    isValidImage = true;
                } else if (fileBytes[0] == (byte)0x89 && fileBytes[1] == (byte)0x50 && 
                          fileBytes[2] == (byte)0x4E && fileBytes[3] == (byte)0x47) {
                    // PNG
                    detectedFormat = "PNG";
                    isValidImage = true;
                } else if (fileBytes[0] == (byte)0x47 && fileBytes[1] == (byte)0x49 && 
                          fileBytes[2] == (byte)0x46) {
                    // GIF
                    detectedFormat = "GIF";
                    isValidImage = true;
                } else if (fileBytes[0] == (byte)0x52 && fileBytes[1] == (byte)0x49 && 
                          fileBytes[2] == (byte)0x46 && fileBytes[3] == (byte)0x46) {
                    // RIFF header - could be WebP or other RIFF format
                    if (fileBytes.length > 11 &&
                        fileBytes[8] == (byte)0x57 && fileBytes[9] == (byte)0x45 && 
                        fileBytes[10] == (byte)0x42 && fileBytes[11] == (byte)0x50) {
                        // WebP
                        detectedFormat = "WebP";
                        isValidImage = true;
                    } else {
                        // RIFF but not WebP - might be incomplete or corrupted
                        detectedFormat = "RIFF (not WebP)";
                        log.warn("File has RIFF header but missing WebP signature for stream. File: {}, Size: {} bytes", 
                                originalFileName, fileBytes.length);
                    }
                }
                
                // Check for format mismatch between content type and detected format
                if (detectedFormat != null && contentType != null) {
                    String expectedFormat = contentType.contains("jpeg") || contentType.contains("jpg") ? "JPEG" :
                                           contentType.contains("png") ? "PNG" :
                                           contentType.contains("gif") ? "GIF" :
                                           contentType.contains("webp") ? "WebP" : null;
                    
                    if (expectedFormat != null && !detectedFormat.equals(expectedFormat) && 
                        !detectedFormat.startsWith("RIFF")) {
                        log.warn("Format mismatch detected for stream. File: {}, Content-Type: {} (expects {}), Actual format: {}", 
                                originalFileName, contentType, expectedFormat, detectedFormat);
                    }
                }
                
                if (!isValidImage) {
                    String hexBytes = String.format("%02X %02X %02X %02X", 
                            fileBytes[0] & 0xFF, fileBytes[1] & 0xFF, 
                            fileBytes[2] & 0xFF, fileBytes[3] & 0xFF);
                    String formatInfo = detectedFormat != null ? 
                            String.format(" Detected format: %s", detectedFormat) : "";
                    log.error("File does not appear to be a valid image based on magic bytes for stream. File: {}, First bytes: {}, Content-Type: {}.{}", 
                            originalFileName, hexBytes, contentType, formatInfo);
                    throw new ImageUploadException(
                            String.format("Invalid image format. File '%s' does not have valid image magic bytes. First bytes: %s. Content-Type: %s.%s The file may be corrupted, incomplete, or not a valid image file.", 
                            originalFileName, hexBytes, contentType, formatInfo));
                }
                
                log.info("Detected image format for stream: {} for file: {} (Content-Type: {})", 
                        detectedFormat, originalFileName, contentType);
                
                // Try to read image with Thumbnailator
                try {
                    originalImage = Thumbnails.of(new ByteArrayInputStream(fileBytes))
                            .size(maxWidth, maxHeight)
                            .asBufferedImage();
                } catch (net.coobird.thumbnailator.tasks.UnsupportedFormatException e) {
                    // If Thumbnailator fails, try Java ImageIO as fallback
                    log.warn("Thumbnailator failed to read image for stream, trying Java ImageIO as fallback. File: {}, Detected format: {}", 
                            originalFileName, detectedFormat);
                    try {
                        javax.imageio.ImageIO.setUseCache(false);
                        originalImage = javax.imageio.ImageIO.read(new ByteArrayInputStream(fileBytes));
                        if (originalImage == null) {
                            // Check if it's a format mismatch issue
                            String formatMismatchMsg = "";
                            if (detectedFormat != null && contentType != null) {
                                boolean isMismatch = (detectedFormat.equals("WebP") && contentType.contains("jpeg")) ||
                                                    (detectedFormat.equals("JPEG") && contentType.contains("webp")) ||
                                                    (detectedFormat.startsWith("RIFF") && !contentType.contains("webp"));
                                if (isMismatch) {
                                    formatMismatchMsg = String.format(" Format mismatch: file appears to be %s but Content-Type is %s.", 
                                            detectedFormat, contentType);
                                }
                            }
                            throw new ImageUploadException(
                                    String.format("Java ImageIO failed to read the image. The file may be corrupted, incomplete, or in an unsupported format.%s Please ensure the file is a valid, complete image file.", 
                                    formatMismatchMsg));
                        }
                        // Resize if needed
                        if (originalImage.getWidth() > maxWidth || originalImage.getHeight() > maxHeight) {
                            double scaleX = (double) maxWidth / originalImage.getWidth();
                            double scaleY = (double) maxHeight / originalImage.getHeight();
                            double scale = Math.min(scaleX, scaleY);
                            int newWidth = (int) (originalImage.getWidth() * scale);
                            int newHeight = (int) (originalImage.getHeight() * scale);
                            originalImage = Thumbnails.of(originalImage)
                                    .size(newWidth, newHeight)
                                    .asBufferedImage();
                        }
                        log.info("Successfully read image using Java ImageIO fallback for stream. Dimensions: {}x{}", 
                                originalImage.getWidth(), originalImage.getHeight());
                    } catch (ImageUploadException imgEx) {
                        // Re-throw ImageUploadException as-is
                        throw imgEx;
                    } catch (Exception ioException) {
                        // Check for format mismatch
                        String formatMismatchMsg = "";
                        if (detectedFormat != null && contentType != null) {
                            boolean isMismatch = (detectedFormat.equals("WebP") && contentType.contains("jpeg")) ||
                                                (detectedFormat.equals("JPEG") && contentType.contains("webp")) ||
                                                (detectedFormat.startsWith("RIFF") && !contentType.contains("webp"));
                            if (isMismatch) {
                                formatMismatchMsg = String.format(" CRITICAL: Format mismatch detected - file appears to be %s but Content-Type is %s. The file extension may be incorrect.", 
                                        detectedFormat, contentType);
                            }
                        }
                        log.error("Both Thumbnailator and Java ImageIO failed to read image for stream. File: {}, Size: {} bytes, Content-Type: {}, Detected format: {}, Error: {}", 
                                originalFileName, fileBytes.length, contentType, detectedFormat, ioException.getMessage(), ioException);
                        throw new ImageUploadException(
                                String.format("Failed to read image data. The file '%s' may be corrupted, incomplete, or in an unsupported format.%s Thumbnailator error: %s, ImageIO error: %s. Please ensure the file is a valid, complete image file and the file extension matches the actual format.", 
                                originalFileName, formatMismatchMsg, e.getMessage(), ioException.getMessage()), e);
                    }
                }
            } catch (ImageUploadException e) {
                // Re-throw ImageUploadException as-is
                throw e;
            } catch (Exception e) {
                log.error("Failed to read image from bytes for stream. File: {}, Size: {} bytes, ContentType: {}, Error: {}", 
                        originalFileName, fileBytes.length, contentType, e.getMessage(), e);
                throw new ImageUploadException("Failed to read image data. The file may be corrupted or in an unsupported format: " + e.getMessage(), e);
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // Determine output format from content type
            String outputFormat = getOutputFormat(contentType);

            // Compress image to target size
            byte[] compressedImageBytes = compressToTargetSize(originalImage, width, height,
                    outputFormat, quality.getQualityPercentage());

            log.info("Compressed image stream ready. Size: {} bytes", compressedImageBytes.length);

            return new ByteArrayInputStream(compressedImageBytes);

        } catch (IOException e) {
            log.error("Error generating compressed image stream", e);
            throw new ImageUploadException("Failed to compress image: " + e.getMessage(), e);
        }
    }

    /**
     * Get compression quality from string
     * @param qualityStr Quality string (HIGH, MEDIUM, LOW)
     * @return CompressionQuality enum value
     */
    public CompressionQuality getCompressionQuality(String qualityStr) {
        if (qualityStr == null || qualityStr.isEmpty()) {
            return CompressionQuality.HIGH;
        }
        try {
            return CompressionQuality.valueOf(qualityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid compression quality: {}, using default HIGH", qualityStr);
            return CompressionQuality.HIGH;
        }
    }

    /**
     * Check if file extension is valid for images
     * @param filename File name to check
     * @return true if extension is valid, false otherwise
     */
    private boolean isValidImageExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * Get output format for Thumbnailator based on content type
     * @param contentType MIME type of image
     * @return Output format string (jpeg, png, gif, webp)
     */
    private String getOutputFormat(String contentType) {
        if (contentType == null) {
            return "jpeg";
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpeg";
        };
    }
}

