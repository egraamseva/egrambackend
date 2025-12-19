package in.gram.gov.app.egram_service.service;

import in.gram.gov.app.egram_service.constants.enums.DocumentCategory;
import in.gram.gov.app.egram_service.constants.enums.Visibility;
import in.gram.gov.app.egram_service.constants.exception.ResourceNotFoundException;
import in.gram.gov.app.egram_service.domain.entity.Document;
import in.gram.gov.app.egram_service.domain.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    private final DocumentRepository documentRepository;

    @Transactional
    public Document create(Document document) {
        log.info("DocumentService.create called - title={}", document.getTitle());
        return documentRepository.save(document);
    }

    public Document findById(Long id) {
        log.info("DocumentService.findById called - id={}", id);
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    public Page<Document> findByPanchayatId(Long panchayatId, Pageable pageable) {
        log.info("DocumentService.findByPanchayatId called - panchayatId={}, pageable={}", panchayatId, pageable);
        return documentRepository.findByPanchayatId(panchayatId, pageable);
    }

    public Page<Document> findByPanchayatIdAndCategory(Long panchayatId, DocumentCategory category, Pageable pageable) {
        log.info("DocumentService.findByPanchayatIdAndCategory called - panchayatId={}, category={}", panchayatId, category);
        return documentRepository.findByPanchayatIdAndCategory(panchayatId, category, pageable);
    }

    public Page<Document> findByPanchayatSlug(String slug, Pageable pageable) {
        log.info("DocumentService.findByPanchayatSlug called - slug={}, pageable={}", slug, pageable);
        return documentRepository.findByPanchayatSlug(slug, pageable);
    }

    public Page<Document> findByPanchayatIdWithFilters(Long panchayatId, DocumentCategory category, Visibility visibility, Pageable pageable) {
        log.info("DocumentService.findByPanchayatIdWithFilters called - panchayatId={}, category={}, visibility={}", 
                panchayatId, category, visibility);
        return documentRepository.findByPanchayatIdWithFilters(panchayatId, category, visibility, pageable);
    }

    public Page<Document> findPublicDocumentsBySlug(String slug, DocumentCategory category, Pageable pageable) {
        log.info("DocumentService.findPublicDocumentsBySlug called - slug={}, category={}", slug, category);
        return documentRepository.findPublicDocumentsBySlug(slug, category, pageable);
    }

    public Page<Document> findWebsiteDocumentsBySlug(String slug, DocumentCategory category, Pageable pageable) {
        log.info("DocumentService.findWebsiteDocumentsBySlug called - slug={}, category={}", slug, category);
        return documentRepository.findWebsiteDocumentsBySlug(slug, category, pageable);
    }

    @Transactional
    public Document update(Document document) {
        log.info("DocumentService.update called - id={}", document.getId());
        return documentRepository.save(document);
    }

    @Transactional
    public void incrementDownloadCount(Long id) {
        log.info("DocumentService.incrementDownloadCount called - id={}", id);
        Document document = findById(id);
        document.setDownloadCount(document.getDownloadCount() + 1);
        documentRepository.save(document);
    }

    @Transactional
    public void delete(Long id) {
        log.info("DocumentService.delete called - id={}", id);
        documentRepository.deleteById(id);
    }
}
