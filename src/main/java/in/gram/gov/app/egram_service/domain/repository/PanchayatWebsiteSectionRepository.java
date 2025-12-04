package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.PanchayatWebsiteSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PanchayatWebsiteSectionRepository extends JpaRepository<PanchayatWebsiteSection, Long> {
    
    @Query("SELECT s FROM PanchayatWebsiteSection s WHERE s.panchayat.id = :panchayatId AND s.isVisible = true ORDER BY s.displayOrder ASC")
    List<PanchayatWebsiteSection> findByPanchayatIdAndVisibleOrdered(@Param("panchayatId") Long panchayatId);
    
    @Query("SELECT s FROM PanchayatWebsiteSection s WHERE s.panchayat.id = :panchayatId ORDER BY s.displayOrder ASC")
    List<PanchayatWebsiteSection> findByPanchayatIdOrdered(@Param("panchayatId") Long panchayatId);
    
    @Query("SELECT s FROM PanchayatWebsiteSection s WHERE s.panchayat.slug = :slug AND s.isVisible = true ORDER BY s.displayOrder ASC")
    List<PanchayatWebsiteSection> findByPanchayatSlugAndVisibleOrdered(@Param("slug") String slug);
}

