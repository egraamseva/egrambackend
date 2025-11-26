# S3CloudStorageService Setup with Backblaze B2

## Overview
You are now using **S3CloudStorageService** with **Backblaze B2 S3-compatible API**. This requires **no AWS account** - only Backblaze B2 credentials.

## What Changed
- ✅ `CloudStorageService` now delegates to `S3CloudStorageService` (instead of BackblazeB2StorageService)
- ✅ Uses Backblaze B2 S3-compatible API endpoint
- ✅ Uses only Backblaze B2 credentials (no AWS needed)

## Step 1: Create Backblaze B2 Account
1. Visit https://www.backblaze.com/b2/cloud-storage/
2. Sign up for a free account (50GB free tier)
3. Create an account

## Step 2: Create a Bucket
1. Log in to Backblaze B2
2. Click **"Buckets"** in the sidebar
3. Click **"Create a Bucket"**
4. Enter bucket name: `egram-service`
5. Set Type to **"Private"**
6. Click **"Create Bucket"**
7. Note the **Bucket ID** (you'll need this)

## Step 3: Generate S3-Compatible API Credentials
This is the **key difference** - you need to generate S3-compatible credentials, NOT the native B2 API key.

### How to Generate S3 Credentials:
1. Log in to Backblaze B2 Dashboard
2. Click on your **account name** (top right)
3. Select **"Account Settings"** or **"Account"**
4. Look for **"App Keys"** section
5. Click **"Add a New Application Key"** or **"Create New S3 Key"**
6. Set the following:
   - **Name**: `egram-s3-key`
   - **Capabilities**: Select "listBuckets", "readBucketInfo", "listFiles", "readFiles", "uploadFile", "deleteFile"
   - **Bucket Restriction**: Select your `egram-service` bucket
   - **File name prefix**: (leave empty for all files)

7. Copy the following:
   - **Access Key ID** (looks like: `001234567890abcdef`)
   - **Secret Access Key** (a long string)

## Step 4: Configure Environment Variables

Add these environment variables to your system or `.env` file:

```bash
# Backblaze B2 Configuration
BACKBLAZE_ENABLED=true
BACKBLAZE_BUCKET_NAME=egram-service
BACKBLAZE_BASE_URL=https://f001.backblazeb2.com
BACKBLAZE_S3_ENDPOINT=https://s3.us-east-005.backblazeb2.com
BACKBLAZE_S3_REGION=us-east-005

# S3-Compatible API Credentials (from Backblaze B2)
BACKBLAZE_S3_ACCESS_KEY=your_access_key_from_step_3
BACKBLAZE_S3_SECRET_KEY=your_secret_key_from_step_3
```

### Environment Variable Reference:
| Variable | Example | Where to Find |
|----------|---------|---------------|
| `BACKBLAZE_S3_ACCESS_KEY` | `001234567890abcdef` | B2 Account > App Keys > S3 Key |
| `BACKBLAZE_S3_SECRET_KEY` | `a1b2c3d4e5f6g7h8i9...` | B2 Account > App Keys > S3 Secret |
| `BACKBLAZE_BUCKET_NAME` | `egram-service` | B2 Buckets page |
| `BACKBLAZE_S3_ENDPOINT` | `https://s3.us-east-005.backblazeb2.com` | Use your B2 region |
| `BACKBLAZE_S3_REGION` | `us-east-005` | See Backblaze B2 regions below |

### Backblaze B2 S3 Endpoints by Region:
```
us-east-005  → https://s3.us-east-005.backblazeb2.com
us-west-000  → https://s3.us-west-000.backblazeb2.com
eu-central-001 → https://s3.eu-central-001.backblazeb2.com
eu-west-002 → https://s3.eu-west-002.backblazeb2.com
ap-northeast-001 → https://s3.ap-northeast-001.backblazeb2.com
```

Check your bucket location in Backblaze B2 Dashboard.

## Step 5: Verify application.yaml Configuration

Your `application.yaml` should have:

```yaml
backblaze:
  b2:
    enabled: ${BACKBLAZE_ENABLED:true}
    bucket-name: ${BACKBLAZE_BUCKET_NAME:egram-service}
    base-url: ${BACKBLAZE_BASE_URL:https://f001.backblazeb2.com}
    s3-endpoint: ${BACKBLAZE_S3_ENDPOINT:https://s3.us-east-005.backblazeb2.com}
    s3-region: ${BACKBLAZE_S3_REGION:us-east-005}
    s3-access-key: ${BACKBLAZE_S3_ACCESS_KEY:}
    s3-secret-key: ${BACKBLAZE_S3_SECRET_KEY:}
```

## Step 6: Test the Integration

### Test Image Upload:
1. Use the `/api/posts` endpoint to create a post with an image
2. The image will be:
   - Compressed (configurable quality)
   - Uploaded to Backblaze B2 via S3-compatible API
   - URL stored in the database

### Check Backblaze B2 Console:
1. Log in to Backblaze B2
2. Go to **Buckets > egram-service > Browse Files**
3. You should see your uploaded images in an `images/` folder

## How It Works

### Image Upload Flow:
```
PostFacade.create()
    ↓
processAndUploadImage()
    ↓
ImageCompressionService.compressImage() [JPEG compression]
    ↓
CloudStorageService.uploadImage()
    ↓
S3CloudStorageService.uploadImageToB2()
    ↓
AWS SDK S3Client (configured with Backblaze B2 endpoint)
    ↓
Backblaze B2 S3-Compatible API
    ↓
File stored in Backblaze B2 bucket
```

### Image URL Format:
```
https://f001.backblazeb2.com/file/egram-service/images/timestamp-uuid.jpg
```

## Troubleshooting

### "Backblaze B2 S3 is not enabled" Error
- Check: `BACKBLAZE_ENABLED=true` is set
- Check: S3 credentials are set correctly

### "403 Forbidden" on Upload
- Verify S3 Access Key and Secret Key are correct
- Verify the key has "uploadFile" capability
- Verify the key is restricted to the correct bucket

### "The specified bucket does not exist" Error
- Verify bucket name matches exactly
- Verify bucket exists in Backblaze B2 console
- Verify S3 endpoint matches bucket region

### "Connection refused"
- Verify S3 endpoint URL is correct (https, not http)
- Verify internet connection is working
- Check if Backblaze B2 service is operational

## Security Best Practices

1. **Never commit credentials** to git - use environment variables only
2. **Rotate keys regularly** - generate new S3 keys and delete old ones in B2 console
3. **Use specific bucket restrictions** - don't create master keys for all buckets
4. **Use the right capabilities** - only grant necessary permissions (no "deleteAll" if not needed)
5. **Store secrets in** `.env` file or secure vault, not in code

## File Structure in Backblaze B2

After uploading images, your bucket will look like:

```
egram-service/
└── images/
    ├── 1700000000000-abc12345.jpg
    ├── 1700000000001-def67890.jpg
    ├── 1700000000002-ghi12345.jpg
    └── ...
```

## Backing Up Credentials

Keep your S3 credentials safe:
- Store in password manager
- Use `.env` file (never commit to git)
- Set `BACKBLAZE_S3_SECRET_KEY` in your deployment platform (Render, Railway, etc.)

## Disabling Cloud Storage

To temporarily disable cloud storage:

```bash
export BACKBLAZE_ENABLED=false
```

When disabled:
- Images won't be uploaded to Backblaze B2
- Posts can still be created without images
- Operations gracefully degrade

## Additional Resources

- [Backblaze B2 Documentation](https://www.backblaze.com/b2/docs/)
- [S3-Compatible API Guide](https://www.backblaze.com/b2/docs/s3_compatible_api.html)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)

