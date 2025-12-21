package in.gram.gov.app.egram_service.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.gram.gov.app.egram_service.constants.enums.LayoutType;
import in.gram.gov.app.egram_service.constants.enums.SectionType;
import in.gram.gov.app.egram_service.constants.security.TenantContext;
import in.gram.gov.app.egram_service.domain.entity.Panchayat;
import in.gram.gov.app.egram_service.domain.entity.PanchayatWebsiteSection;
import in.gram.gov.app.egram_service.domain.entity.SectionTemplate;
import in.gram.gov.app.egram_service.dto.response.PanchayatWebsiteSectionResponseDTO;
import in.gram.gov.app.egram_service.dto.response.SectionTemplateResponseDTO;
import in.gram.gov.app.egram_service.service.PanchayatService;
import in.gram.gov.app.egram_service.service.PanchayatWebsiteSectionService;
import in.gram.gov.app.egram_service.service.SectionTemplateService;
import in.gram.gov.app.egram_service.transformer.PanchayatWebsiteSectionTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionTemplateFacade {
    private final SectionTemplateService templateService;
    private final PanchayatWebsiteSectionService sectionService;
    private final PanchayatService panchayatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SectionTemplateResponseDTO> getAllTemplates(String language) {
        log.info("SectionTemplateFacade.getAllTemplates called - language={}", language);
        List<SectionTemplate> templates = templateService.findAllActive(language);
        return templates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PanchayatWebsiteSectionResponseDTO> createPageFromTemplate(Long templateId) {
        log.info("SectionTemplateFacade.createPageFromTemplate called - templateId={}", templateId);
        
        SectionTemplate template = templateService.findById(templateId);
        if (!template.getIsPageTemplate()) {
            throw new IllegalArgumentException("Template is not a page template");
        }

        Long panchayatId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(panchayatId);

        // Parse template data
        JsonNode templateData;
        try {
            templateData = objectMapper.readTree(template.getTemplateData());
        } catch (Exception e) {
            log.error("Failed to parse template data", e);
            throw new RuntimeException("Invalid template data format", e);
        }

        // Get sections array from template
        JsonNode sectionsNode = templateData.get("sections");
        if (sectionsNode == null || !sectionsNode.isArray()) {
            throw new RuntimeException("Template data must contain a 'sections' array");
        }

        List<PanchayatWebsiteSection> createdSections = new ArrayList<>();
        int displayOrder = 0;

        // Get current max display order
        List<PanchayatWebsiteSection> existingSections = sectionService.findByPanchayatId(panchayatId);
        int maxOrder = existingSections.stream()
                .mapToInt(PanchayatWebsiteSection::getDisplayOrder)
                .max()
                .orElse(-1);
        displayOrder = maxOrder + 1;

        // Create each section from template
        for (JsonNode sectionNode : sectionsNode) {
            try {
                PanchayatWebsiteSection section = createSectionFromTemplateNode(sectionNode, panchayat, displayOrder);
                section = sectionService.create(section);
                createdSections.add(section);
                displayOrder++;
            } catch (Exception e) {
                log.error("Failed to create section from template", e);
                // Continue with next section
            }
        }

        log.info("Created {} sections from template", createdSections.size());
        return createdSections.stream()
                .map(PanchayatWebsiteSectionTransformer::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PanchayatWebsiteSectionResponseDTO createSectionFromTemplate(Long templateId, Integer displayOrder, Boolean isVisible) {
        log.info("SectionTemplateFacade.createSectionFromTemplate called - templateId={}, displayOrder={}", templateId, displayOrder);
        
        SectionTemplate template = templateService.findById(templateId);
        if (template.getIsPageTemplate()) {
            throw new IllegalArgumentException("Template is a page template. Use createPageFromTemplate instead.");
        }

        Long panchayatId = TenantContext.getTenantId();
        Panchayat panchayat = panchayatService.findById(panchayatId);

        // Parse template data - for single section templates, the templateData is the section itself
        JsonNode sectionNode;
        try {
            sectionNode = objectMapper.readTree(template.getTemplateData());
        } catch (Exception e) {
            log.error("Failed to parse template data", e);
            throw new RuntimeException("Invalid template data format", e);
        }

        // If displayOrder not provided, set to max + 1
        if (displayOrder == null) {
            List<PanchayatWebsiteSection> existingSections = sectionService.findByPanchayatId(panchayatId);
            int maxOrder = existingSections.stream()
                    .mapToInt(PanchayatWebsiteSection::getDisplayOrder)
                    .max()
                    .orElse(-1);
            displayOrder = maxOrder + 1;
        }

        PanchayatWebsiteSection section = createSectionFromTemplateNode(sectionNode, panchayat, displayOrder);
        if (isVisible != null) {
            section.setIsVisible(isVisible);
        }
        
        section = sectionService.create(section);
        log.info("Created section from template with ID: {}", section.getId());
        return PanchayatWebsiteSectionTransformer.toDTO(section);
    }

    private PanchayatWebsiteSection createSectionFromTemplateNode(JsonNode sectionNode, Panchayat panchayat, int displayOrder) {
        // Replace placeholders in the section data
        String sectionJson = replacePlaceholders(sectionNode.toString(), panchayat);

        // Parse the processed JSON
        JsonNode processedNode;
        try {
            processedNode = objectMapper.readTree(sectionJson);
        } catch (Exception e) {
            log.error("Failed to parse processed section data", e);
            processedNode = sectionNode; // Fallback to original
        }

        // Extract section properties
        String sectionTypeStr = processedNode.has("sectionType") ? processedNode.get("sectionType").asText() : null;
        String layoutTypeStr = processedNode.has("layoutType") ? processedNode.get("layoutType").asText() : null;
        String title = processedNode.has("title") ? processedNode.get("title").asText() : null;
        String subtitle = processedNode.has("subtitle") ? processedNode.get("subtitle").asText() : null;
        String backgroundColor = processedNode.has("backgroundColor") ? processedNode.get("backgroundColor").asText() : null;
        String textColor = processedNode.has("textColor") ? processedNode.get("textColor").asText() : null;
        Boolean isVisible = processedNode.has("isVisible") ? processedNode.get("isVisible").asBoolean() : true;

        // Get content (everything except the section metadata)
        final ObjectNode contentNode;
        if (processedNode.has("content")) {
            contentNode = (ObjectNode) processedNode.get("content");
        } else {
            // If no content node, copy all fields except section metadata
            contentNode = objectMapper.createObjectNode();
            processedNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!key.equals("sectionType") && !key.equals("layoutType") && 
                    !key.equals("title") && !key.equals("subtitle") && 
                    !key.equals("backgroundColor") && !key.equals("textColor") && 
                    !key.equals("isVisible") && !key.equals("displayOrder")) {
                    contentNode.set(key, entry.getValue());
                }
            });
        }

        // Create section entity
        PanchayatWebsiteSection section = PanchayatWebsiteSection.builder()
                .panchayat(panchayat)
                .sectionType(SectionType.valueOf(sectionTypeStr))
                .layoutType(LayoutType.valueOf(layoutTypeStr))
                .title(title)
                .subtitle(subtitle)
                .content(contentNode.toString())
                .displayOrder(displayOrder)
                .isVisible(isVisible)
                .backgroundColor(backgroundColor)
                .textColor(textColor)
                .build();

        return section;
    }

    private String replacePlaceholders(String json, Panchayat panchayat) {
        // Replace common placeholders with actual panchayat data
        String result = json;
        
        // Replace [Panchayat Name]
        if (panchayat.getPanchayatName() != null) {
            result = result.replace("[Panchayat Name]", panchayat.getPanchayatName());
            result = result.replace("[पंचायत नाम]", panchayat.getPanchayatName());
            result = result.replace("[पंचायत नाव]", panchayat.getPanchayatName());
        }
        
        // Replace [Sarpanch Name] - you may need to fetch this from team members
        // For now, using a placeholder
        result = result.replace("[Sarpanch Name]", "Sarpanch");
        result = result.replace("[सरपंच नाम]", "सरपंच");
        result = result.replace("[सरपंच नाव]", "सरपंच");
        
        // Replace [Up-Sarpanch Name]
        result = result.replace("[Up-Sarpanch Name]", "Up-Sarpanch");
        result = result.replace("[उप-सरपंच नाम]", "उप-सरपंच");
        result = result.replace("[उप-सरपंच नाव]", "उप-सरपंच");
        
        // Replace [Ward Member Name]
        result = result.replace("[Ward Member Name]", "Ward Member");
        result = result.replace("[वार्ड सदस्य नाम]", "वार्ड सदस्य");
        result = result.replace("[वार्ड सदस्य नाव]", "वार्ड सदस्य");
        
        // Replace district and state
        if (panchayat.getDistrict() != null) {
            result = result.replace("[District]", panchayat.getDistrict());
        }
        if (panchayat.getState() != null) {
            result = result.replace("[State]", panchayat.getState());
        }
        
        return result;
    }

    private SectionTemplateResponseDTO toDTO(SectionTemplate template) {
        SectionTemplateResponseDTO dto = new SectionTemplateResponseDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setCategory(template.getCategory());
        dto.setLanguage(template.getLanguage());
        dto.setColorTheme(template.getColorTheme());
        dto.setPreviewImageUrl(template.getPreviewImageUrl());
        dto.setIsActive(template.getIsActive());
        dto.setIsSystem(template.getIsSystem());
        dto.setIsPageTemplate(template.getIsPageTemplate());
        dto.setDisplayOrder(template.getDisplayOrder());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        
        // Parse templateData JSON
        try {
            dto.setTemplateData(objectMapper.readTree(template.getTemplateData()));
        } catch (Exception e) {
            log.warn("Failed to parse template data as JSON", e);
            dto.setTemplateData(template.getTemplateData());
        }
        
        return dto;
    }
}

