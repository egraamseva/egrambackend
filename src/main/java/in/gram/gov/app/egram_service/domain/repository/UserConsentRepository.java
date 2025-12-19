package in.gram.gov.app.egram_service.domain.repository;

import in.gram.gov.app.egram_service.domain.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    
    @Query("SELECT c FROM UserConsent c WHERE c.user.id = :userId " +
           "AND c.consentGiven = true AND c.revokedAt IS NULL " +
           "ORDER BY c.consentTimestamp DESC")
    Optional<UserConsent> findActiveConsentByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM UserConsent c WHERE c.user.id = :userId " +
           "ORDER BY c.consentTimestamp DESC")
    Optional<UserConsent> findLatestConsentByUserId(@Param("userId") Long userId);
}

