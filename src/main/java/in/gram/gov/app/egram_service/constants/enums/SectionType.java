package in.gram.gov.app.egram_service.constants.enums;

/**
 * Section types for both Platform Landing Page and Panchayat Website sections.
 * Some types are shared between both platforms (e.g., GALLERY, FAQ, FORM, VIDEO, etc.)
 */
public enum SectionType {
    // Platform Landing Page Section Types
    HERO,
    STATS,
    FEATURES,
    ACTIVE_PANCHAYATS,
    NEWS,
    CTA,
    
    // Panchayat Website Section Types
    ANNOUNCEMENTS,
    SCHEMES,
    MEMBERS,
    CONTACT,
    
    // Shared Section Types (available for both Platform and Panchayat)
    GALLERY,
    CARDS,
    FAQ,
    FORM,
    VIDEO,
    TIMELINE,
    TESTIMONIALS,
    RICH_TEXT,
    MAP
}

