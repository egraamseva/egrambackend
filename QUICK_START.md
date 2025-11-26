# Quick Start: S3 with Backblaze B2 (No AWS)

## TL;DR Setup (5 minutes)

### 1. Create Free Backblaze B2 Account
```
Go to: https://www.backblaze.com/b2/cloud-storage/
Sign up (free tier: 50GB)
```

### 2. Create Bucket
```
Account > Buckets > Create Bucket
Name: egram-service
Type: Private
Save Bucket ID
```

### 3. Generate S3 Credentials
```
Account > App Keys > Create S3 Key
Name: egram-s3-key
Capabilities: listBuckets, listFiles, readFiles, uploadFile, deleteFile
Bucket: egram-service
Copy: Access Key ID and Secret Access Key
```

### 4. Set Environment Variables
```bash
export BACKBLAZE_ENABLED=true
export BACKBLAZE_BUCKET_NAME=egram-service
export BACKBLAZE_S3_ACCESS_KEY=YOUR_ACCESS_KEY
export BACKBLAZE_S3_SECRET_KEY=YOUR_SECRET_KEY
export BACKBLAZE_S3_ENDPOINT=https://s3.us-east-005.backblazeb2.com
export BACKBLAZE_S3_REGION=us-east-005
```

### 5. Run Your App
```bash
mvn spring-boot:run
```

### 6. Test Upload
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "title=Test Post" \
  -F "bodyText=Test body" \
  -F "imageFile=@path/to/image.jpg"
```

## What's Different from AWS?

| Aspect | AWS S3 | Backblaze B2 + S3 API |
|--------|--------|----------------------|
| Account Signup | Requires credit card | Free tier available |
| Credentials | Access Key/Secret Key | S3-compatible key |
| Cost | More expensive | Cheaper |
| Setup Time | Longer | Faster |
| Endpoint | `s3.amazonaws.com` | `s3.us-east-005.backblazeb2.com` |

**The code is identical** - we just use Backblaze's S3-compatible endpoint instead of AWS!

## File Locations

| File | Purpose |
|------|---------|
| `S3_BACKBLAZE_SETUP_GUIDE.md` | Complete detailed setup |
| `CHANGES_MADE.md` | What was changed and why |
| `application.yaml` | Configuration with env vars |

## Verify Setup

### In application.yaml:
```yaml
backblaze:
  b2:
    enabled: ${BACKBLAZE_ENABLED:true}
    s3-access-key: ${BACKBLAZE_S3_ACCESS_KEY:}
    s3-secret-key: ${BACKBLAZE_S3_SECRET_KEY:}
```

### In S3CloudStorageService.java:
```java
@Value("${backblaze.b2.s3-endpoint:https://s3.us-east-005.backblazeb2.com}")
private String s3Endpoint;

AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
    s3AccessKey,    // Backblaze S3 key
    s3SecretKey     // Backblaze S3 secret
);

S3Client s3Client = S3Client.builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
    .endpointOverride(URI.create(s3Endpoint))  // Backblaze B2 endpoint
    .build();
```

The AWS SDK is simply pointing to Backblaze's S3-compatible API instead of AWS!

## Troubleshooting

### "Cloud storage is not enabled"
```
Check: export BACKBLAZE_ENABLED=true
Check: Env var is set before running app
```

### "Credentials not configured"
```
Check: export BACKBLAZE_S3_ACCESS_KEY=...
Check: export BACKBLAZE_S3_SECRET_KEY=...
```

### "The specified bucket does not exist"
```
Check: Bucket name matches exactly: egram-service
Check: Bucket exists in Backblaze B2 console
Check: S3 endpoint matches bucket region (e.g., us-east-005)
```

### "403 Forbidden"
```
Check: S3 credentials are correct
Check: S3 key has "uploadFile" capability
Check: S3 key is restricted to "egram-service" bucket
```

## Next Steps

1. Read `S3_BACKBLAZE_SETUP_GUIDE.md` for detailed setup
2. Set up Backblaze B2 account (5 min)
3. Generate S3 credentials (2 min)
4. Set environment variables
5. Test with a POST request
6. Check Backblaze B2 console to see uploaded files

Done! 🎉

