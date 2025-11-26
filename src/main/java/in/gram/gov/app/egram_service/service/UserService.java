package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.enums.UserRole;
import in.gram.gov.app.egram_service.constants.enums.UserStatus;
import in.gram.gov.app.egram_service.constants.exception.DuplicateResourceException;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.User;
import in.gram.gov.app.egram_service.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User create(User user) {
        log.info("UserService.create called - email={}", user.getEmail());
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User with email " + user.getEmail() + " already exists");
        }
        return userRepository.save(user);
    }

    public User findById(Long id) {
        log.debug("UserService.findById called - id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User findByEmail(String email) {
        log.debug("UserService.findByEmail called - email={}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
    }

    public User findByEmailOrNull(String email) {
        log.debug("UserService.findByEmailOrNull called - email={}", email);
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<User> findByPanchayatIdAndRole(Long panchayatId, UserRole role) {
        log.info("UserService.findByPanchayatIdAndRole called - panchayatId={}, role={}", panchayatId, role);
        return userRepository.findByPanchayatIdAndRole(panchayatId, role);
    }

    public Long countByPanchayatIdAndRoleAndStatus(Long panchayatId, UserRole role, UserStatus status) {
        log.debug("UserService.countByPanchayatIdAndRoleAndStatus called - panchayatId={}, role={}, status={}", panchayatId, role, status);
        return userRepository.countByPanchayatIdAndRoleAndStatus(panchayatId, role, status);
    }

    public Page<User> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("UserService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return userRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<User> findByFilters(Long panchayatId, UserRole role, UserStatus status, Pageable pageable) {
        log.info("UserService.findByFilters called - panchayatId={}, role={}, status={}, pageable={}", panchayatId, role, status, pageable);
        return userRepository.findByFilters(panchayatId, role, status, pageable);
    }

    @Transactional
    public User update(User user) {
        log.info("UserService.update called - id={}", user.getId());
        return userRepository.save(user);
    }

    @Transactional
    public void updateStatus(Long id, UserStatus status) {
        log.info("UserService.updateStatus called - id={}, status={}", id, status);
        User user = findById(id);
        user.setStatus(status);
        userRepository.save(user);
    }

    public User findByPasswordResetToken(String token) {
        log.debug("UserService.findByPasswordResetToken called - tokenPresent={}", token != null);
        return userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired password reset token"));
    }
}
