package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long>, JpaSpecificationExecutor<Album> {
    @Query("SELECT a FROM Album a WHERE a.panchayat.id = :panchayatId")
    Page<Album> findByPanchayatId(@Param("panchayatId") Long panchayatId, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.panchayat.slug = :slug")
    Page<Album> findByPanchayatSlug(@Param("slug") String slug, Pageable pageable);
}

