package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.Newsletter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {
    
    @Query("SELECT n FROM Newsletter n WHERE n.panchayat.id = :panchayatId")
    Page<Newsletter> findByPanchayatId(@Param("panchayatId") Long panchayatId, Pageable pageable);
    
    @Query("SELECT n FROM Newsletter n WHERE n.panchayat.slug = :slug AND n.isPublished = true")
    Page<Newsletter> findByPanchayatSlugAndPublished(@Param("slug") String slug, Pageable pageable);
    
    @Query("SELECT n FROM Newsletter n WHERE n.panchayat.slug = :slug AND n.isPublished = true AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.subtitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Newsletter> findByPanchayatSlugAndPublishedWithSearch(
            @Param("slug") String slug, 
            @Param("search") String search, 
            Pageable pageable);
    
    @Query("SELECT n FROM Newsletter n WHERE n.panchayat.id = :panchayatId AND n.isPublished = :isPublished")
    Page<Newsletter> findByPanchayatIdAndIsPublished(
            @Param("panchayatId") Long panchayatId, 
            @Param("isPublished") Boolean isPublished, 
            Pageable pageable);
}

