package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.PlatformLandingPageSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformLandingPageSectionRepository extends JpaRepository<PlatformLandingPageSection, Long> {
    
    @Query("SELECT s FROM PlatformLandingPageSection s WHERE s.isVisible = true ORDER BY s.displayOrder ASC")
    List<PlatformLandingPageSection> findAllVisibleOrdered();
    
    @Query("SELECT s FROM PlatformLandingPageSection s ORDER BY s.displayOrder ASC")
    List<PlatformLandingPageSection> findAllOrdered();
}

