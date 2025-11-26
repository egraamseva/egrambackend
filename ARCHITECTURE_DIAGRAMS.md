# Architecture & Flow Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend/API Client                      │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REST API Layer                                │
├─────────────────────────────────────────────────────────────────┤
│ PanchayatAlbumController                                         │
│  ├── POST   /albums                  - Create album             │
│  ├── GET    /albums                  - List albums              │
│  ├── GET    /albums/{id}             - Get album                │
│  ├── PUT    /albums/{id}             - Update album             │
│  ├── POST   /albums/{id}/images      - ⭐ ADD IMAGES (NEW)      │
│  └── DELETE /albums/{id}             - Delete album             │
│                                                                  │
│ PanchayatGalleryController                                       │
│  ├── POST   /gallery                 - Create/upload image      │
│  ├── GET    /gallery                 - List images (by album)   │
│  ├── GET    /gallery/{id}            - Get image                │
│  ├── PUT    /gallery/{id}            - Update image/move album  │
│  └── DELETE /gallery/{id}            - Delete image             │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Facade Layer                                │
├─────────────────────────────────────────────────────────────────┤
│ AlbumFacade (AlbumFacadeNew)                                     │
│  ├── create()                                                    │
│  ├── getById()                                                   │
│  ├── getAll()                                                    │
│  ├── update()                                                    │
│  ├── delete()                                                    │
│  ├── refreshCoverImageUrl()                                      │
│  └── addImagesToAlbum()          ⭐ NEW METHOD                   │
│                                                                  │
│ GalleryImageFacade                                               │
│  ├── create()                                                    │
│  ├── getById()                                                   │
│  ├── getAll()                                                    │
│  ├── update()                                                    │
│  ├── delete()                                                    │
│  └── refreshImageUrl()                                           │
└─────────────────┬───────────────────────────────────────────────┘
                  │
         ┌────────┴────────┐
         ▼                 ▼
┌────────────────────┐  ┌──────────────────────────┐
│  Service Layer     │  │  Other Services          │
├────────────────────┤  ├──────────────────────────┤
│ AlbumService       │  │ ImageCompressionService  │
│ GalleryImageService│  │ CloudStorageService      │
│ PanchayatService   │  │ UserService              │
│ UserService        │  │ TenantContext            │
└────────────────────┘  └──────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Access Layer                             │
├─────────────────────────────────────────────────────────────────┤
│ AlbumRepository           - Find, Save, Delete Albums            │
│ GalleryImageRepository    - Find, Save, Delete Images            │
│ PanchayatRepository       - Find Panchayats                      │
│ UserRepository            - Find Users                           │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Database                                    │
├─────────────────────────────────────────────────────────────────┤
│ albums table               - Album records                       │
│ gallery_images table       - Gallery image records (FK: album)   │
│ panchayat table            - Panchayat records                   │
│ users table                - User records                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Bulk Add Images Flow

```
Client Request
    │
    ▼
POST /api/v1/panchayat/albums/{id}/images
  ├── { "galleryImageIds": [1, 2, 3, 4, 5] }
    │
    ▼
PanchayatAlbumController.addImagesToAlbum()
    ├── Validate input (not null, not empty)
    │    └─ ❌ If empty: Return 400 Bad Request
    │
    ├── ✅ Log operation start
    │
    ▼
AlbumFacade.addImagesToAlbum(albumId, imageIds)
    │ @Transactional
    │
    ├── Load Album from database
    │    └─ ❌ If not found: Throw exception → 404
    │
    ├── For each imageId in list:
    │   │
    │   ├── Try {
    │   │   ├── Load GalleryImage from database
    │   │   ├── Check: Is it already in this album?
    │   │   │   ├─ Yes: Skip it (prevent duplicate)
    │   │   │   └─ No: Continue...
    │   │   ├── Set image.album = album
    │   │   ├── Save image to database
    │   │   ├── addedCount++
    │   │   └── Log success
    │   │ } Catch {
    │   │   └── Log warning (but continue with next image)
    │   │ }
    │
    ├── End transaction (commit or rollback)
    │
    ▼
Return response with:
    ├── ✅ success: true
    ├── message: "Successfully added N images"
    └── data:
        ├── albumId: 1
        ├── imagesAdded: N (actual count)
        └── totalRequested: M (requested count)
    │
    ▼
Client receives JSON response
```

---

## Database Relationship Diagram

```
┌─────────────────────┐
│   panchayats        │
├─────────────────────┤
│ id (PK)             │
│ name                │
│ slug                │
│ ...                 │
└──────────┬──────────┘
           │
           │ 1:N (One Panchayat has Many Albums)
           │
           ▼
┌─────────────────────┐         ┌──────────────────────┐
│   albums            │◄────────┤   gallery_images     │
├─────────────────────┤         ├──────────────────────┤
│ id (PK)             │         │ id (PK)              │
│ panchayat_id (FK)   │         │ panchayat_id (FK)    │
│ album_name          │         │ album_id (FK) ✅     │
│ description         │         │ uploaded_by_user_id  │
│ cover_image_url     │         │ image_url            │
│ created_at          │         │ caption              │
│ updated_at          │         │ tags                 │
└─────────────────────┘         │ display_order        │
           ▲                     │ created_at           │
           │                     │ updated_at           │
           │ 1:N (OneToMany)     └──────────────────────┘
           │ (mappedBy: "album")   │
           │ images List<...>      │
           │                       │
           └───────────────────────┘

Relationship Notes:
- Album.images = @OneToMany(mappedBy="album")
- GalleryImage.album = @ManyToOne(nullable=true)
- Cascade delete ON album removes images
- Gallery images can exist without album (nullable)
```

---

## Transaction Flow (Detailed)

```
START TRANSACTION
    │
    ├── BEGIN
    │
    ├── SELECT album FROM albums WHERE id = ?
    │   └─ Load Album entity into memory
    │
    ├── FOR EACH imageId in [1, 2, 3, 4, 5]:
    │   │
    │   ├─ SELECT image FROM gallery_images WHERE id = ?
    │   │  └─ Load GalleryImage entity
    │   │
    │   ├─ IF (image.album == null || image.album.id != albumId) THEN
    │   │  │
    │   │  ├─ UPDATE gallery_images
    │   │  │  SET album_id = ?
    │   │  │  WHERE id = ?
    │   │  │  └─ Associate image with album
    │   │  │
    │   │  └─ addedCount++
    │   │
    │   └─ END IF
    │
    ├── COMMIT TRANSACTION ✅
    │   └─ All changes persisted to database
    │
    └─ Return addedCount

IF ERROR during transaction:
    ├── ROLLBACK TRANSACTION ⚠️
    │   └─ All changes reverted
    └─ Throw exception to caller
```

---

## Error Handling Flow

```
REQUEST
  │
  ▼
VALIDATE INPUT
  ├─ Is galleryImageIds null? ──YES──> RETURN 400 Bad Request
  ├─ Is galleryImageIds empty? ─YES──> RETURN 400 Bad Request
  └─ NO ──> CONTINUE
  │
  ▼
LOAD ALBUM
  ├─ SELECT * FROM albums WHERE id = ?
  ├─ Album found? ──NO──> THROW EntityNotFoundException
  │   └─ Global handler catches ──> RETURN 404 Not Found
  └─ Album found? ──YES──> CONTINUE
  │
  ▼
FOR EACH IMAGE_ID
  │
  ├─ TRY {
  │   │
  │   ├─ LOAD IMAGE
  │   │  ├─ SELECT * FROM gallery_images WHERE id = ?
  │   │  ├─ Image found? ──NO──> THROW EntityNotFoundException
  │   │  │   └─ CATCH ──> LOG WARNING ──> CONTINUE NEXT
  │   │  └─ Image found? ──YES──> CONTINUE
  │   │
  │   ├─ CHECK DUPLICATE
  │   │  ├─ Is already in album? ──YES──> SKIP ──> NEXT IMAGE
  │   │  └─ Is NOT in album? ──YES──> UPDATE ──> addedCount++
  │   │
  │   └─ SUCCESS ──> LOG INFO
  │ }
  │
  └─ CATCH {
     ├─ Any exception caught
     ├─ LOG WARNING (with exception details)
     └─ CONTINUE WITH NEXT IMAGE
     }
  │
  ▼
COMMIT TRANSACTION
  │
  ▼
BUILD RESPONSE
  └─ Return {
       success: true,
       message: "Successfully added X images",
       data: {
         albumId: ...,
         imagesAdded: X,
         totalRequested: Y
       }
     }
```

---

## Data Flow - Add Images Request

```
1. CLIENT SENDS REQUEST
   POST /api/v1/panchayat/albums/1/images
   {
     "galleryImageIds": [10, 11, 12]
   }

2. REQUEST RECEIVED BY CONTROLLER
   PanchayatAlbumController.addImagesToAlbum()
       │
       ├─ Extract pathVariable: albumId = 1
       ├─ Extract requestBody: payload = Map<String, List<Long>>
       └─ Extract: galleryImageIds = [10, 11, 12]

3. CONTROLLER DELEGATES TO FACADE
   AlbumFacade.addImagesToAlbum(1, [10, 11, 12])
       │
       ├─ Load Album(1) from DB
       ├─ Initialize: addedCount = 0
       │
       └─ Loop through each image:
          │
          Image 10:
          ├─ Load GalleryImage(10) from DB
          ├─ Check: album == null? YES
          ├─ Set: image.album = Album(1)
          ├─ Save image to DB
          ├─ addedCount = 1 ✓
          │
          Image 11:
          ├─ Load GalleryImage(11) from DB
          ├─ Check: album == null? YES
          ├─ Set: image.album = Album(1)
          ├─ Save image to DB
          ├─ addedCount = 2 ✓
          │
          Image 12:
          ├─ Load GalleryImage(12) from DB
          ├─ Check: album == null? YES
          ├─ Set: image.album = Album(1)
          ├─ Save image to DB
          ├─ addedCount = 3 ✓

4. RESPONSE BUILT
   {
     "success": true,
     "message": "Successfully added 3 images to album",
     "data": {
       "albumId": 1,
       "imagesAdded": 3,
       "totalRequested": 3
     }
   }

5. RESPONSE SENT TO CLIENT
   HTTP 200 OK
   [JSON response above]
```

---

## State Diagram - Gallery Image States

```
              CREATE IMAGE
                   │
                   ▼
         ┌────────────────────┐
         │   UNASSOCIATED     │
         │  (no album)        │
         └────────┬───────────┘
                  │
        ┌─────────┼─────────┐
        │         │         │
        │ ADD TO  │ UPDATE  │ DELETE
        │ ALBUM   │ ALBUM   │  IMAGE
        │         │         │
        ▼         ▼         ▼
    ┌──────────────────┐  [DELETED]
    │  ASSOCIATED      │
    │  (album_id !=0)  │
    └──────────────────┘
         │      │
         │      ├─ CHANGE ALBUM
         │      │  (album_id = new_id)
         │      │
         │      └─ REMOVE FROM ALBUM
         │         (album_id = null)
         │
         └────────────────────┐
                             │
                  ┌──────────▼──────┐
                  │  DELETE ALBUM   │
                  │  ↓              │
                  │ ALBUM DELETED   │
                  │ IMAGE REMAINS   │
                  │ (album_id = null)
                  └─────────────────┘
```

---

## Component Interaction Diagram

```
                    ┌─────────────────────┐
                    │ PanchayatAlbumCtrl  │
                    └──────────┬──────────┘
                               │
                   ┌───────────▼────────────┐
                   │                        │
                   ▼                        ▼
        ┌─────────────────────┐  ┌───────────────────┐
        │ AlbumFacade         │  │ Validation        │
        │ • addImagesToAlbum()│  │ • Empty check     │
        └──────────┬──────────┘  └───────────────────┘
                   │
        ┌──────────┼──────────┐
        │          │          │
        ▼          ▼          ▼
    ┌─────┐  ┌─────────┐  ┌──────────────┐
    │Album│  │Gallery  │  │Image         │
    │Service │ │ImageSvc │  │Compression  │
    └─────┘  └─────────┘  │Service (idle)│
        │          │       └──────────────┘
        ▼          ▼
    ┌─────────────────────────┐
    │ Database                │
    │ • albums table          │
    │ • gallery_images table  │
    └─────────────────────────┘
```

---

## Class Hierarchy

```
┌─────────────────────────────────────┐
│ BaseEntity                          │ (Superclass)
├─────────────────────────────────────┤
│ + id                                │
│ + createdAt                         │
│ + updatedAt                         │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
┌──────────────┐  ┌───────────────────┐
│ Album        │  │ GalleryImage      │
├──────────────┤  ├───────────────────┤
│ id           │  │ id                │
│ panchayat_id │  │ panchayat_id      │
│ album_name   │  │ album_id ✅ (FK)   │
│ description  │  │ uploaded_by_user_id
│ cover_image_ │  │ image_url         │
│   url        │  │ caption           │
│ images ◄─────┼──┤ tags              │
│              │  │ display_order     │
└──────────────┘  └───────────────────┘

Relationship:
Album (1) ◄────── (Many) GalleryImage
  └─ OneToMany mapping via images field
```

---

This architecture ensures:
- ✅ Clean separation of concerns
- ✅ Transaction safety
- ✅ Proper error handling
- ✅ Scalability
- ✅ Maintainability
- ✅ Security through role-based access

