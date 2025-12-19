package in.gram.gov.app.egram_service.service;

import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import in.gram.gov.app.egram_service.constants.exception.BadRequestException;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.constants.exception.UnauthorizedException;
import in.gram.gov.app.egram_service.domain.entity.GoogleDriveToken;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.repository.GoogleDriveTokenRepository;
import in.gram.gov.app.egram_service.domain.repository.PanchayatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveService {
    private final GoogleDriveTokenRepository tokenRepository;
    private final PanchayatRepository panchayatRepository;
    private final TokenEncryptionService encryptionService;
    private final ConsentService consentService;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.drive.scope:https://www.googleapis.com/auth/drive.file}")
    private String driveScope;

    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Get authorization URL for OAuth flow
     * Uses panchayat ID for token storage (panchayat email should be used for OAuth)
     */
    public String getAuthorizationUrl(Long panchayatId) {
        log.info("GoogleDriveService.getAuthorizationUrl called - panchayatId={}", panchayatId);

        GoogleAuthorizationCodeFlow flow = createAuthorizationCodeFlow();
        GoogleAuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(String.valueOf(panchayatId)); // Pass panchayatId in state for callback

        return authorizationUrl.build();
    }

    /**
     * Handle OAuth callback and store tokens
     * Stores tokens per panchayat (panchayat email should be used for OAuth)
     */
    @Transactional
    public void handleOAuthCallback(String code, String state) throws IOException {
        log.info("GoogleDriveService.handleOAuthCallback called - state={}", state);

        Long panchayatId = Long.parseLong(state);
        Panchayat panchayat = panchayatRepository.findById(panchayatId)
                .orElseThrow(() -> new ResourceNotFoundException("Panchayat", panchayatId));

        GoogleAuthorizationCodeFlow flow = createAuthorizationCodeFlow();
        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        // Calculate expiry time (typically 3600 seconds)
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(
                response.getExpiresInSeconds() != null ? response.getExpiresInSeconds() : 3600
        );

        // Encrypt tokens before storing
        String encryptedAccessToken = encryptionService.encrypt(response.getAccessToken());
        String encryptedRefreshToken = response.getRefreshToken() != null
                ? encryptionService.encrypt(response.getRefreshToken())
                : null;

        // Save or update token
        GoogleDriveToken token = tokenRepository.findByPanchayatId(panchayatId)
                .orElse(GoogleDriveToken.builder()
                        .panchayat(panchayat)
                        .build());

        token.setAccessToken(encryptedAccessToken);
        token.setRefreshToken(encryptedRefreshToken);
        token.setExpiryTime(expiryTime);
        token.setScope(driveScope);
        token.setTokenType("Bearer");

        tokenRepository.save(token);
        log.info("GoogleDriveService.handleOAuthCallback - tokens saved for panchayatId={}", panchayatId);
    }

    /**
     * Upload file to panchayat's Google Drive in "egram" folder
     */
    public String uploadFile(MultipartFile file, Long panchayatId, String category) throws IOException {
        log.info("GoogleDriveService.uploadFile called - panchayatId={}, fileName={}, category={}",
                panchayatId, file.getOriginalFilename(), category);

        Drive driveService = getDriveService(panchayatId);

        // Get or create "egram" folder
        String folderId = getOrCreateEgramFolder(driveService);

        // Create file metadata
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        fileMetadata.setMimeType(file.getContentType());
        fileMetadata.setParents(Collections.singletonList(folderId)); // Set parent folder

        // Upload file content
        InputStream fileContent = new ByteArrayInputStream(file.getBytes());
        try {
            File uploadedFile = driveService.files().create(fileMetadata,
                            new com.google.api.client.http.InputStreamContent(
                                    file.getContentType(), fileContent))
                    .setFields("id, name, mimeType, size, webViewLink, webContentLink")
                    .execute();

            // Share file with "anyone with the link" to make it accessible
            try {
                shareFileWithAnyone(uploadedFile.getId(), driveService);
                log.info("GoogleDriveService.uploadFile - file shared with 'anyone with the link'. fileId={}", uploadedFile.getId());
            } catch (Exception e) {
                log.error("GoogleDriveService.uploadFile - failed to share file, but upload succeeded. fileId={}, error={}", 
                        uploadedFile.getId(), e.getMessage(), e);
                // Continue even if sharing fails - file is still uploaded
            }

            log.info("GoogleDriveService.uploadFile - file uploaded successfully. fileId={}, fileName={}, folderId={}",
                    uploadedFile.getId(), uploadedFile.getName(), folderId);

            return uploadedFile.getId();
        } catch (GoogleJsonResponseException e) {
            log.error("Google Drive API error: {}", e.getMessage(), e);
            
            // Check for specific error codes
            if (e.getStatusCode() == 403) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("SERVICE_DISABLED")) {
                    throw new BadRequestException(
                        "Google Drive API is not enabled in your Google Cloud project. " +
                        "Please enable it at: https://console.cloud.google.com/apis/api/drive.googleapis.com/overview " +
                        "and wait a few minutes for the changes to propagate."
                    );
                } else if (errorMessage != null && errorMessage.contains("accessNotConfigured")) {
                    throw new BadRequestException(
                        "Google Drive API is not enabled. " +
                        "Please enable it in Google Cloud Console: https://console.cloud.google.com/apis/api/drive.googleapis.com/overview"
                    );
                }
            }
            
            // Generic Google API error
            throw new BadRequestException(
                "Google Drive API error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error") +
                ". Please check your Google Cloud Console settings."
            );
        }
    }

    /**
     * Get file metadata from Google Drive
     */
    public File getFileMetadata(String fileId, Long panchayatId) throws IOException {
        log.debug("GoogleDriveService.getFileMetadata called - fileId={}, panchayatId={}", fileId, panchayatId);

        Drive driveService = getDriveService(panchayatId);
        return driveService.files().get(fileId)
                .setFields("id, name, mimeType, size, webViewLink, webContentLink, createdTime, modifiedTime")
                .execute();
    }

    /**
     * Create a view link for the file (preview URL)
     * Also ensures file permissions are set correctly
     */
    public String createViewLink(String fileId, Long panchayatId) throws IOException {
        log.debug("GoogleDriveService.createViewLink called - fileId={}, panchayatId={}", fileId, panchayatId);

        Drive driveService = getDriveService(panchayatId);
        
        // Ensure file permissions are set correctly before getting view link
        try {
            shareFileWithAnyone(fileId, driveService);
        } catch (Exception e) {
            log.warn("GoogleDriveService.createViewLink - failed to update permissions, but continuing. fileId={}, error={}", 
                    fileId, e.getMessage());
            // Continue - try to get view link anyway
        }

        File file = getFileMetadata(fileId, panchayatId);
        if (file.getWebViewLink() != null) {
            return file.getWebViewLink();
        } else if (file.getWebContentLink() != null) {
            return file.getWebContentLink();
        } else {
            // Fallback: construct a view URL
            return "https://drive.google.com/file/d/" + fileId + "/view";
        }
    }

    /**
     * Delete file from Google Drive
     */
    public void deleteFile(String fileId, Long panchayatId) throws IOException {
        log.info("GoogleDriveService.deleteFile called - fileId={}, panchayatId={}", fileId, panchayatId);

        Drive driveService = getDriveService(panchayatId);
        driveService.files().delete(fileId).execute();
        log.info("GoogleDriveService.deleteFile - file deleted successfully. fileId={}", fileId);
    }

    /**
     * Check if file is available in Drive
     */
    public boolean checkFileAvailability(String fileId, Long panchayatId) {
        try {
            getFileMetadata(fileId, panchayatId);
            return true;
        } catch (Exception e) {
            log.warn("GoogleDriveService.checkFileAvailability - file not available. fileId={}, error={}",
                    fileId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if Google Drive is connected for panchayat
     */
    public boolean isConnected(Long panchayatId) {
        log.debug("GoogleDriveService.isConnected called - panchayatId={}", panchayatId);
        return tokenRepository.findByPanchayatId(panchayatId).isPresent();
    }

    /**
     * Revoke Google Drive access
     */
    @Transactional
    public void revokeAccess(Long panchayatId) {
        log.info("GoogleDriveService.revokeAccess called - panchayatId={}", panchayatId);

        tokenRepository.findByPanchayatId(panchayatId).ifPresent(token -> {
            try {
                // Revoke token with Google by making direct HTTP call
                String accessToken = encryptionService.decrypt(token.getAccessToken());
                if (accessToken != null) {
                    try {
                        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
                        // Google's revoke endpoint accepts token as query parameter
                        GenericUrl revokeUrl = new GenericUrl("https://oauth2.googleapis.com/revoke?token=" + 
                                URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
                        com.google.api.client.http.HttpRequest request = requestFactory.buildGetRequest(revokeUrl);
                        request.execute();
                        log.info("GoogleDriveService.revokeAccess - token revoked with Google for panchayatId={}", panchayatId);
                    } catch (IOException e) {
                        log.warn("Error revoking token with Google: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Error getting token for revocation: {}", e.getMessage());
            }

            // Delete token from database
            tokenRepository.delete(token);
            log.info("GoogleDriveService.revokeAccess - token deleted for panchayatId={}", panchayatId);
        });
    }

    /**
     * Get Drive service instance for panchayat
     */
    private Drive getDriveService(Long panchayatId) throws IOException {
        Credential credential = getCredential(panchayatId);
        if (credential == null) {
            throw new UnauthorizedException("Google Drive access not authorized. Please connect your Google Drive.");
        }

        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("E-GramSeva")
                .build();
    }

    /**
     * Get credential for panchayat, refreshing if needed
     */
    private Credential getCredential(Long panchayatId) throws IOException {
        GoogleDriveToken token = tokenRepository.findByPanchayatId(panchayatId)
                .orElseThrow(() -> new UnauthorizedException("Google Drive not connected"));

        // Decrypt tokens
        String accessToken = encryptionService.decrypt(token.getAccessToken());
        String refreshToken = token.getRefreshToken() != null
                ? encryptionService.decrypt(token.getRefreshToken())
                : null;

        // Build credential from stored tokens
        Credential credential = new Credential.Builder(
                com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .setClientAuthentication(new ClientParametersAuthentication(
                        clientId, clientSecret))
                .build();

        credential.setAccessToken(accessToken);
        if (refreshToken != null) {
            credential.setRefreshToken(refreshToken);
        }
        credential.setExpirationTimeMilliseconds(
                token.getExpiryTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        );

        // Refresh token if needed
        if (token.needsRefresh() && refreshToken != null) {
            try {
                credential.refreshToken();

                // Update stored token after refresh
                token.setAccessToken(encryptionService.encrypt(credential.getAccessToken()));
                if (credential.getRefreshToken() != null) {
                    token.setRefreshToken(encryptionService.encrypt(credential.getRefreshToken()));
                }
                token.setExpiryTime(LocalDateTime.now().plusSeconds(3600));
                tokenRepository.save(token);

                log.info("GoogleDriveService.getCredential - token refreshed for panchayatId={}", panchayatId);
            } catch (TokenResponseException e) {
                log.error("Error refreshing token: {}", e.getMessage());
                throw new UnauthorizedException("Failed to refresh Google Drive access. Please reconnect.");
            } catch (IOException e) {
                log.error("IO error refreshing token: {}", e.getMessage());
                throw new UnauthorizedException("Failed to refresh Google Drive access. Please reconnect.");
            }
        }

        return credential;
    }

    /**
     * Share file with "anyone with the link" permission
     */
    private void shareFileWithAnyone(String fileId, Drive driveService) throws IOException {
        try {
            Permission permission = new Permission();
            permission.setType("anyone");
            permission.setRole("reader");
            permission.setAllowFileDiscovery(false); // Only accessible via link, not searchable
            
            driveService.permissions().create(fileId, permission)
                    .setFields("id")
                    .execute();
            log.debug("GoogleDriveService.shareFileWithAnyone - file shared successfully. fileId={}", fileId);
        } catch (GoogleJsonResponseException e) {
            // If permission already exists, that's okay
            if (e.getStatusCode() == 400 && e.getMessage() != null && 
                e.getMessage().contains("Permission already exists")) {
                log.debug("GoogleDriveService.shareFileWithAnyone - permission already exists. fileId={}", fileId);
            } else {
                log.warn("GoogleDriveService.shareFileWithAnyone - failed to share file. fileId={}, error={}", 
                        fileId, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Update file permissions to make it accessible to anyone with the link
     * This can be called to fix permissions for existing files
     */
    public void updateFilePermissions(String fileId, Long panchayatId) throws IOException {
        log.info("GoogleDriveService.updateFilePermissions called - fileId={}, panchayatId={}", fileId, panchayatId);
        
        Drive driveService = getDriveService(panchayatId);
        shareFileWithAnyone(fileId, driveService);
        
        log.info("GoogleDriveService.updateFilePermissions - permissions updated successfully. fileId={}", fileId);
    }

    /**
     * Get or create "egram" folder in Google Drive
     */
    private String getOrCreateEgramFolder(Drive driveService) throws IOException {
        String folderName = "egram";
        
        try {
            // Search for existing folder
            FileList result = driveService.files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();
            
            List<File> folders = result.getFiles();
            if (folders != null && !folders.isEmpty()) {
                // Folder exists, return its ID
                String folderId = folders.get(0).getId();
                log.debug("Found existing 'egram' folder with ID: {}", folderId);
                return folderId;
            }
            
            // Folder doesn't exist, create it
            File folderMetadata = new File();
            folderMetadata.setName(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            
            File folder = driveService.files().create(folderMetadata)
                    .setFields("id, name")
                    .execute();
            
            log.info("Created 'egram' folder with ID: {}", folder.getId());
            return folder.getId();
            
        } catch (GoogleJsonResponseException e) {
            log.error("Error getting/creating 'egram' folder: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create or access 'egram' folder in Google Drive: " + e.getMessage());
        }
    }

    /**
     * Create authorization code flow
     */
    private GoogleAuthorizationCodeFlow createAuthorizationCodeFlow() {
        return new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientId,
                clientSecret,
                Collections.singletonList(DriveScopes.DRIVE_FILE))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }
}

