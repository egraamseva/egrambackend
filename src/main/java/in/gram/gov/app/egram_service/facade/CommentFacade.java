package in.gram.gov.app.egram_service.facade;

import in.gram.gov.app.egram_service.domain.entity.Comment;
import in.gram.gov.app.egram_service.domain.entity.Post;
import in.gram.gov.app.egram_service.dto.request.CommentRequestDTO;
import in.gram.gov.app.egram_service.dto.response.CommentResponseDTO;
import in.gram.gov.app.egram_service.service.CommentService;
import in.gram.gov.app.egram_service.service.PostService;
import in.gram.gov.app.egram_service.transformer.CommentTransformer;
import jakarta.servlet.http.HttpServletRequest;
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
public class CommentFacade {
    private final CommentService commentService;
    private final PostService postService;

    @Transactional
    public CommentResponseDTO create(Long postId, CommentRequestDTO request, HttpServletRequest httpRequest) {
        log.info("CommentFacade.create called - postId={}, ipHeaderPresent={}, parentId={}", postId, httpRequest.getHeader("X-Forwarded-For") != null, request.getParentCommentId());
        Post post = postService.findById(postId);
        
        Comment comment = CommentTransformer.toEntity(request);
        comment.setPost(post);
        comment.setIpAddress(getClientIpAddress(httpRequest));

        if (request.getParentCommentId() != null) {
            Comment parent = commentService.findById(request.getParentCommentId());
            comment.setParentComment(parent);
        }

        comment = commentService.create(comment);
        return CommentTransformer.toDTO(comment);
    }

    public Page<CommentResponseDTO> getPostComments(Long postId, Pageable pageable, boolean approvedOnly) {
        log.info("CommentFacade.getPostComments called - postId={}, approvedOnly={}, pageable={}", postId, approvedOnly, pageable);
        Page<Comment> comments = approvedOnly
                ? commentService.findApprovedByPostId(postId, pageable)
                : commentService.findTopLevelByPostId(postId, pageable);
        
        return comments.map(comment -> {
            List<Comment> replies = commentService.findRepliesByParentId(comment.getId());
            return CommentTransformer.toDTOWithReplies(comment, replies);
        });
    }

    @Transactional
    public void approve(Long postId, Long commentId) {
        log.info("CommentFacade.approve called - postId={}, commentId={}", postId, commentId);
        Comment comment = commentService.findById(commentId);
        if (!comment.getPost().getId().equals(postId)) {
            throw new RuntimeException("Comment does not belong to this post");
        }
        commentService.approve(commentId);
    }

    @Transactional
    public void delete(Long postId, Long commentId) {
        log.info("CommentFacade.delete called - postId={}, commentId={}", postId, commentId);
        Comment comment = commentService.findById(commentId);
        if (!comment.getPost().getId().equals(postId)) {
            throw new RuntimeException("Comment does not belong to this post");
        }
        commentService.delete(commentId);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
