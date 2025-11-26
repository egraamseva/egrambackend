package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Comment;
import in.gram.gov.app.egram_service.domain.repository.CommentRepository;
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
public class CommentService {
    private final CommentRepository commentRepository;

    @Transactional
    public Comment create(Comment comment) {
        log.info("CommentService.create called - comment={}", comment);
        return commentRepository.save(comment);
    }

    public Comment findById(Long id) {
        log.info("CommentService.findById called - id={}", id);
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }

    public Page<Comment> findTopLevelByPostId(Long postId, Pageable pageable) {
        log.info("CommentService.findTopLevelByPostId called - postId={}, pageable={}", postId, pageable);
        return commentRepository.findTopLevelByPostId(postId, pageable);
    }

    public Page<Comment> findApprovedByPostId(Long postId, Pageable pageable) {
        log.info("CommentService.findApprovedByPostId called - postId={}, pageable={}", postId, pageable);
        return commentRepository.findApprovedByPostId(postId, pageable);
    }

    public List<Comment> findAllByPostId(Long postId) {
        log.info("CommentService.findAllByPostId called - postId={}", postId);
        return commentRepository.findAllByPostId(postId);
    }

    public List<Comment> findRepliesByParentId(Long parentId) {
        log.info("CommentService.findRepliesByParentId called - parentId={}", parentId);
        return commentRepository.findRepliesByParentId(parentId);
    }

    @Transactional
    public Comment update(Comment comment) {
        log.info("CommentService.update called - comment={}", comment);
        return commentRepository.save(comment);
    }

    @Transactional
    public void approve(Long id) {
        log.info("CommentService.approve called - id={}", id);
        Comment comment = findById(id);
        comment.setApprovedFlag(true);
        commentRepository.save(comment);
    }

    @Transactional
    public void delete(Long id) {
        log.info("CommentService.delete called - id={}", id);
        commentRepository.deleteById(id);
    }
}
