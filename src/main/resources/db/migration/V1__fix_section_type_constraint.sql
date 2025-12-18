-- Migration to fix section_type constraint to include CONTENT_SECTION
-- This ensures the constraint matches the SectionType enum

-- Update panchayat_website_sections table constraint
DO $$
BEGIN
    -- Drop existing section_type constraint if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'panchayat_website_sections_section_type_check'
    ) THEN
        ALTER TABLE panchayat_website_sections 
        DROP CONSTRAINT panchayat_website_sections_section_type_check;
    END IF;
END $$;

-- Create new section_type constraint with all enum values (including CONTENT_SECTION)
ALTER TABLE panchayat_website_sections
ADD CONSTRAINT panchayat_website_sections_section_type_check
CHECK (section_type IN (
    -- Legacy types
    'HERO',
    'STATS',
    'FEATURES',
    'ACTIVE_PANCHAYATS',
    'NEWS',
    'CTA',
    'ANNOUNCEMENTS',
    'SCHEMES',
    'MEMBERS',
    'CONTACT',
    'GALLERY',
    'CARDS',
    'FAQ',
    'FORM',
    'VIDEO',
    'TIMELINE',
    'TESTIMONIALS',
    'RICH_TEXT',
    'MAP',
    -- New professional types
    'HERO_BANNER',
    'PARAGRAPH_CONTENT',
    'CONTENT_SECTION',
    'IMAGE_WITH_TEXT',
    'SPLIT_CONTENT',
    'IMAGE_GALLERY',
    'VIDEO_SECTION',
    'CARD_SECTION',
    'CARD_GRID',
    'FEATURES_GRID',
    'STATISTICS_SECTION',
    'TEAM_MEMBERS',
    'FAQ_SECTION',
    'FORM_SECTION',
    'TESTIMONIALS_SECTION',
    'TIMELINE_SECTION',
    'NEWS_FEED',
    'SCHEMES_LIST',
    'CONTACT_INFO',
    'MAP_SECTION',
    'CALL_TO_ACTION',
    'ACTIVE_PANCHAYATS_GRID'
));

-- Update platform_landing_page_sections table constraint
DO $$
BEGIN
    -- Drop existing section_type constraint if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'platform_landing_page_sections_section_type_check'
    ) THEN
        ALTER TABLE platform_landing_page_sections 
        DROP CONSTRAINT platform_landing_page_sections_section_type_check;
    END IF;
END $$;

-- Create new section_type constraint with all enum values (including CONTENT_SECTION)
ALTER TABLE platform_landing_page_sections
ADD CONSTRAINT platform_landing_page_sections_section_type_check
CHECK (section_type IN (
    -- Legacy types
    'HERO',
    'STATS',
    'FEATURES',
    'ACTIVE_PANCHAYATS',
    'NEWS',
    'CTA',
    'ANNOUNCEMENTS',
    'SCHEMES',
    'MEMBERS',
    'CONTACT',
    'GALLERY',
    'CARDS',
    'FAQ',
    'FORM',
    'VIDEO',
    'TIMELINE',
    'TESTIMONIALS',
    'RICH_TEXT',
    'MAP',
    -- New professional types
    'HERO_BANNER',
    'PARAGRAPH_CONTENT',
    'CONTENT_SECTION',
    'IMAGE_WITH_TEXT',
    'SPLIT_CONTENT',
    'IMAGE_GALLERY',
    'VIDEO_SECTION',
    'CARD_SECTION',
    'CARD_GRID',
    'FEATURES_GRID',
    'STATISTICS_SECTION',
    'TEAM_MEMBERS',
    'FAQ_SECTION',
    'FORM_SECTION',
    'TESTIMONIALS_SECTION',
    'TIMELINE_SECTION',
    'NEWS_FEED',
    'SCHEMES_LIST',
    'CONTACT_INFO',
    'MAP_SECTION',
    'CALL_TO_ACTION',
    'ACTIVE_PANCHAYATS_GRID'
));

