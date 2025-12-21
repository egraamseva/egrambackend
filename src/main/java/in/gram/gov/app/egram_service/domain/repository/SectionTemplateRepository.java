package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.SectionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionTemplateRepository extends JpaRepository<SectionTemplate, Long> {
    
    @Query("SELECT t FROM SectionTemplate t WHERE t.isActive = true AND (:language IS NULL OR t.language = :language) ORDER BY t.displayOrder ASC, t.name ASC")
    List<SectionTemplate> findByLanguageAndActive(@Param("language") String language);
    
    @Query("SELECT t FROM SectionTemplate t WHERE t.isActive = true AND t.isPageTemplate = true AND (:language IS NULL OR t.language = :language) ORDER BY t.displayOrder ASC, t.name ASC")
    List<SectionTemplate> findPageTemplatesByLanguage(@Param("language") String language);
    
    @Query("SELECT t FROM SectionTemplate t WHERE t.isActive = true AND t.isPageTemplate = false AND (:language IS NULL OR t.language = :language) ORDER BY t.displayOrder ASC, t.name ASC")
    List<SectionTemplate> findSectionTemplatesByLanguage(@Param("language") String language);
    
    Optional<SectionTemplate> findByIdAndIsActiveTrue(Long id);
}

