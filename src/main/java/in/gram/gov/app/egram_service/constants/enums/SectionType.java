package in.gram.gov.app.egram_service.constants.enums;


public enum SectionType {
    // ============================================
    // Professional Generic Section Types
    // ============================================

    // Hero & Banner Sections
    HERO_BANNER,

    // Content Sections
    PARAGRAPH_CONTENT,
    IMAGE_WITH_TEXT,
    SPLIT_CONTENT,

    // Media Sections
    IMAGE_GALLERY,
    VIDEO_SECTION,

    // Card & Grid Sections
    CARD_SECTION,
    FEATURES_GRID,
    STATISTICS_SECTION,
    TEAM_MEMBERS,

    // Interactive Sections
    FAQ_SECTION,
    FORM_SECTION,
    TESTIMONIALS_SECTION,
    TIMELINE_SECTION,

    // Specialized Sections
    NEWS_FEED,
    SCHEMES_LIST,
    CONTACT_INFO,
    MAP_SECTION,
    CALL_TO_ACTION,
    ACTIVE_PANCHAYATS_GRID,

    // ============================================
    // Legacy Section Types (Backward Compatibility)
    // ============================================

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