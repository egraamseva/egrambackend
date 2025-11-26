package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.domain.entity.Like;
import in.gram.gov.app.egram_service.domain.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {
    private final LikeRepository likeRepository;

    @Transactional
    public Like create(Like like) {
        log.info("LikeService.create called - postId={}, userId={}", like.getPost() != null ? like.getPost().getId() : null, like.getUser() != null ? like.getUser().getId() : null);
        return likeRepository.save(like);
    }

    public Optional<Like> findByPostIdAndUserId(Long postId, Long userId) {
        log.debug("LikeService.findByPostIdAndUserId called - postId={}, userId={}", postId, userId);
        return likeRepository.findByPostIdAndUserId(postId, userId);
    }

    public Optional<Like> findByPostIdAndVisitorIdentifier(Long postId, String visitorIdentifier) {
        log.debug("LikeService.findByPostIdAndVisitorIdentifier called - postId={}, visitorIdentifier={}", postId, visitorIdentifier);
        return likeRepository.findByPostIdAndVisitorIdentifier(postId, visitorIdentifier);
    }

    public Long countByPostId(Long postId) {
        log.debug("LikeService.countByPostId called - postId={}", postId);
        return likeRepository.countByPostId(postId);
    }

    @Transactional
    public void delete(Long id) {
        log.info("LikeService.delete called - id={}", id);
        likeRepository.deleteById(id);
    }
}
