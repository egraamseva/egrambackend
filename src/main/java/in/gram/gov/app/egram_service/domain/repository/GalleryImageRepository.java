package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.GalleryImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long>, JpaSpecificationExecutor<GalleryImage> {
    @Query("SELECT g FROM GalleryImage g WHERE g.panchayat.id = :panchayatId")
    Page<GalleryImage> findByPanchayatId(@Param("panchayatId") Long panchayatId, Pageable pageable);

    @Query("SELECT g FROM GalleryImage g WHERE g.panchayat.id = :panchayatId AND " +
            "(:albumId IS NULL OR g.album.id = :albumId)")
    Page<GalleryImage> findByPanchayatIdAndAlbumId(@Param("panchayatId") Long panchayatId,
                                                   @Param("albumId") Long albumId,
                                                   Pageable pageable);

    @Query("SELECT g FROM GalleryImage g WHERE g.panchayat.slug = :slug AND " +
            "(:albumId IS NULL OR g.album.id = :albumId) ORDER BY g.displayOrder ASC, g.createdAt DESC")
    Page<GalleryImage> findByPanchayatSlugAndAlbumId(@Param("slug") String slug,
                                                     @Param("albumId") Long albumId,
                                                     Pageable pageable);
}

