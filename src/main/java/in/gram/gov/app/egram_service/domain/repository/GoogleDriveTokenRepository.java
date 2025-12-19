package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.GoogleDriveToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleDriveTokenRepository extends JpaRepository<GoogleDriveToken, Long> {
    
    Optional<GoogleDriveToken> findByPanchayatId(Long panchayatId);
    
    @Query("SELECT t FROM GoogleDriveToken t WHERE t.panchayat.id = :panchayatId")
    Optional<GoogleDriveToken> findByPanchayat_Id(@Param("panchayatId") Long panchayatId);
    
    void deleteByPanchayatId(Long panchayatId);
}

