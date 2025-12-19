package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.domain.entity.UserConsent;
import in.gram.gov.app.egram_service.domain.repository.UserConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {
    private final UserConsentRepository consentRepository;

    @Transactional
    public UserConsent recordConsent(Long userId, String ipAddress) {
        log.info("ConsentService.recordConsent called - userId={}, ipAddress={}", userId, ipAddress);
        
        // Revoke any existing active consent
        consentRepository.findActiveConsentByUserId(userId)
                .ifPresent(existingConsent -> {
                    existingConsent.setRevokedAt(LocalDateTime.now());
                    existingConsent.setConsentGiven(false);
                    consentRepository.save(existingConsent);
                });

        UserConsent consent = UserConsent.builder()
                .user(User.builder().id(userId).build())
                .consentGiven(true)
                .consentTimestamp(LocalDateTime.now())
                .purpose("Google Drive document storage for Panchayat documents")
                .ipAddress(ipAddress)
                .build();

        return consentRepository.save(consent);
    }

    public boolean hasValidConsent(Long userId) {
        log.debug("ConsentService.hasValidConsent called - userId={}", userId);
        return consentRepository.findActiveConsentByUserId(userId).isPresent();
    }

    public UserConsent getActiveConsent(Long userId) {
        log.debug("ConsentService.getActiveConsent called - userId={}", userId);
        return consentRepository.findActiveConsentByUserId(userId)
                .orElse(null);
    }

    @Transactional
    public void revokeConsent(Long userId, String reason) {
        log.info("ConsentService.revokeConsent called - userId={}, reason={}", userId, reason);
        
        consentRepository.findActiveConsentByUserId(userId)
                .ifPresent(consent -> {
                    consent.setRevokedAt(LocalDateTime.now());
                    consent.setConsentGiven(false);
                    consent.setRevokeReason(reason);
                    consentRepository.save(consent);
                });
    }
}

