package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import in.gram.gov.app.egram_service.domain.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT d FROM Document d WHERE d.panchayat.id = :panchayatId")
    Page<Document> findByPanchayatId(@Param("panchayatId") Long panchayatId, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.panchayat.id = :panchayatId AND " +
           "(:category IS NULL OR d.category = :category)")
    Page<Document> findByPanchayatIdAndCategory(@Param("panchayatId") Long panchayatId,
                                                 @Param("category") DocumentCategory category,
                                                 Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.panchayat.slug = :slug ORDER BY d.createdAt DESC")
    Page<Document> findByPanchayatSlug(@Param("slug") String slug, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.panchayat.id = :panchayatId " +
           "AND (:category IS NULL OR d.category = :category) " +
           "AND (:visibility IS NULL OR d.visibility = :visibility) " +
           "AND d.isAvailable = true " +
           "ORDER BY d.createdAt DESC")
    Page<Document> findByPanchayatIdWithFilters(@Param("panchayatId") Long panchayatId,
                                                 @Param("category") DocumentCategory category,
                                                 @Param("visibility") Visibility visibility,
                                                 Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.panchayat.slug = :slug " +
           "AND d.visibility = 'PUBLIC' " +
           "AND d.isAvailable = true " +
           "AND (:category IS NULL OR d.category = :category) " +
           "ORDER BY d.createdAt DESC")
    Page<Document> findPublicDocumentsBySlug(@Param("slug") String slug,
                                             @Param("category") DocumentCategory category,
                                             Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.panchayat.slug = :slug " +
           "AND d.showOnWebsite = true " +
           "AND d.isAvailable = true " +
           "AND (:category IS NULL OR d.category = :category) " +
           "ORDER BY d.createdAt DESC")
    Page<Document> findWebsiteDocumentsBySlug(@Param("slug") String slug,
                                              @Param("category") DocumentCategory category,
                                              Pageable pageable);
    
    Optional<Document> findByGoogleDriveFileId(String googleDriveFileId);
    
    @Query("SELECT d FROM Document d WHERE d.googleDriveFileId = :fileId AND d.uploadedBy.id = :userId")
    Optional<Document> findByGoogleDriveFileIdAndUserId(@Param("fileId") String fileId,
                                                         @Param("userId") Long userId);
}

