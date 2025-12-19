# Google Drive Document Management Implementation

## Overview
This implementation provides a complete document management system where documents are stored in users' Google Drive accounts, NOT on the application server. Only metadata is stored in the database.

## ✅ Completed Implementation

### Backend Components

#### 1. Dependencies Added
- Google Drive API v3
- Google API Client
- Google OAuth Client
- Google Auth Library

#### 2. Database Entities
- **UserConsent**: Tracks user consent for Google Drive access
- **GoogleDriveToken**: Stores encrypted OAuth tokens
- **Document**: Updated with Google Drive fields (googleDriveFileId, visibility, etc.)

#### 3. Services
- **TokenEncryptionService**: AES-256 encryption for OAuth tokens
- **ConsentService**: Manages user consent lifecycle
- **GoogleDriveService**: Handles OAuth flow and Drive operations
- **DocumentService**: Updated with new query methods

#### 4. Controllers
- **ConsentController**: `/api/v1/consent/*` - Consent management
- **GoogleOAuthController**: `/api/v1/auth/google/*` - OAuth flow
- **PanchayatDocumentController**: `/api/v1/panchayat/documents/*` - Document CRUD
- **PublicController**: Updated with public document endpoints

#### 5. Database Migration
- `V2__create_google_drive_tables.sql` - Creates all necessary tables

### Frontend Components

#### 1. ConsentDialog Component
- Matches existing design system
- Clear consent explanation
- Privacy notices

#### 2. DocumentsManagement Component
- Updated with Google Drive integration
- Consent checking before upload
- Google Drive connection flow
- Document viewer integration

#### 3. DocumentViewer Component
- Google Drive preview in iframe
- Fallback handling
- Download and external link options

#### 4. API Service Updates
- `consentAPI`: Consent management
- `googleOAuthAPI`: OAuth flow
- `documentsAPI`: Updated with Google Drive endpoints

## 🔧 Configuration Required

### 1. Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Google Drive API**
4. Create **OAuth 2.0 Client ID**:
   - Application type: Web application
   - Authorized redirect URIs: `https://your-domain.com/api/v1/auth/google/callback`
   - Scopes: `https://www.googleapis.com/auth/drive.file`

### 2. Environment Variables

Add to your backend environment:

```bash
# Google OAuth Configuration
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=https://your-domain.com/api/v1/auth/google/callback
BASE_URL=https://your-domain.com

# Encryption Key (generate a 256-bit key and base64 encode it)
GOOGLE_ENCRYPTION_KEY=your-base64-encoded-256-bit-key
```

**To generate encryption key:**
```bash
# Using OpenSSL
openssl rand -base64 32
```

### 3. Database Migration

Run Flyway migration:
```bash
mvn flyway:migrate
```

Or the migration will run automatically on application startup.

## 📋 API Endpoints

### Consent Management
- `POST /api/v1/consent/grant` - Grant consent
- `GET /api/v1/consent/status` - Check consent status
- `POST /api/v1/consent/revoke` - Revoke consent

### Google OAuth
- `GET /api/v1/auth/google/authorize` - Get authorization URL
- `GET /api/v1/auth/google/callback` - OAuth callback (handles redirect)
- `POST /api/v1/auth/google/revoke` - Revoke Google access

### Document Management
- `POST /api/v1/panchayat/documents` - Upload document (multipart/form-data)
- `GET /api/v1/panchayat/documents` - List documents (with filters)
- `GET /api/v1/panchayat/documents/{id}` - Get document metadata
- `GET /api/v1/panchayat/documents/{id}/view` - Get view link
- `PATCH /api/v1/panchayat/documents/{id}/visibility` - Update visibility
- `DELETE /api/v1/panchayat/documents/{id}` - Delete document

### Public Endpoints
- `GET /api/v1/public/{slug}/documents` - List public documents
- `GET /api/v1/public/{slug}/documents/{id}/view` - View public document

## 🔒 Security Features

1. **Consent First**: All Drive operations require explicit consent
2. **Limited Scope**: Uses `drive.file` scope (only app-created files)
3. **Token Encryption**: AES-256 encryption for stored tokens
4. **Token Refresh**: Automatic refresh before expiry
5. **Access Control**: Visibility-based access (PUBLIC/PRIVATE)
6. **Ownership Validation**: Users can only modify/delete their own documents

## 🎯 User Flow

1. **First Upload**:
   - User clicks "Upload Document"
   - Consent dialog appears
   - User grants consent
   - Redirected to Google OAuth
   - Authorizes Drive access
   - Returns to app
   - Can now upload documents

2. **Subsequent Uploads**:
   - Direct upload to Google Drive
   - File stored in user's Drive
   - Metadata saved in database

3. **Viewing Documents**:
   - Click "View" on document
   - Opens Google Drive preview
   - Fallback if preview unavailable

## 📝 Document Categories

- NOTICE
- REPORT
- BUDGET
- TENDER
- FORM
- RESOLUTION
- OTHER

## 🎨 UI Design

All components match existing design system:
- Color scheme: `#E31E24` (primary red), `#1B2B5E` (navy blue)
- Typography: Existing font scales
- Components: Radix UI components
- Responsive: Mobile-first design
- Icons: Lucide React

## ⚠️ Important Notes

1. **Never Store Files**: Files are uploaded directly to Drive, never to server
2. **Consent Required**: All Drive operations blocked until consent granted
3. **Token Security**: Encryption key must be kept secure
4. **OAuth Callback**: Must match exactly in Google Console
5. **Scope Limitation**: `drive.file` scope ensures privacy
6. **Error Handling**: Graceful degradation if Drive unavailable

## 🧪 Testing Checklist

- [ ] User can grant consent
- [ ] OAuth flow completes successfully
- [ ] Documents upload to Google Drive
- [ ] Documents appear in user's Drive
- [ ] View links work correctly
- [ ] Public documents accessible without auth
- [ ] Private documents require ownership
- [ ] Token refresh works automatically
- [ ] Consent revocation works
- [ ] Document deletion removes from Drive
- [ ] Error messages are user-friendly

## 🚀 Deployment Steps

1. Set environment variables in production
2. Run database migration
3. Configure Google OAuth redirect URI
4. Test OAuth flow end-to-end
5. Verify file uploads appear in Drive
6. Test public document access
7. Monitor token refresh logs

## 📚 Additional Resources

- [Google Drive API Documentation](https://developers.google.com/drive/api/v3/about-sdk)
- [OAuth 2.0 for Web Applications](https://developers.google.com/identity/protocols/oauth2/web-server)
- [Drive File Scope](https://developers.google.com/drive/api/v3/about-auth)

