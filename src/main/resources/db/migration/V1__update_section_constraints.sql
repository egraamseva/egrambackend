-- Migration to update CHECK constraints for layout_type and section_type columns
-- This allows the new enum values (MASONRY, LIST, SPLIT, FULL_WIDTH, CONTAINED for layouts
-- and GALLERY, CARDS, FAQ, FORM, VIDEO, TIMELINE, TESTIMONIALS, RICH_TEXT, MAP for sections)

-- Update panchayat_website_sections table constraints
DO $$
BEGIN
    -- Drop existing layout_type constraint if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'panchayat_website_sections_layout_type_check'
    ) THEN
        ALTER TABLE panchayat_website_sections 
        DROP CONSTRAINT panchayat_website_sections_layout_type_check;
    END IF;

    -- Drop existing section_type constraint if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'panchayat_website_sections_section_type_check'
    ) THEN
        ALTER TABLE panchayat_website_sections 
        DROP CONSTRAINT panchayat_website_sections_section_type_check;
    END IF;
END $$;

-- Create new layout_type constraint with all enum values
ALTER TABLE panchayat_website_sections
ADD CONSTRAINT panchayat_website_sections_layout_type_check
CHECK (layout_type IN (
    'GRID',
    'ROW',
    'SCROLLING_ROW',
    'CAROUSEL',
    'MASONRY',
    'LIST',
    'SPLIT',
    'FULL_WIDTH',
    'CONTAINED'
));

-- Create new section_type constraint with all enum values (including new professional types)
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
    'IMAGE_WITH_TEXT',
    'SPLIT_CONTENT',
    'IMAGE_GALLERY',
    'VIDEO_SECTION',
    'CARD_SECTION',
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

-- Update platform_landing_page_sections table constraints
DO $$
BEGIN
    -- Drop existing layout_type constraint if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'platform_landing_page_sections_layout_type_check'
    ) THEN
        ALTER TABLE platform_landing_page_sections 
        DROP CONSTRAINT platform_landing_page_sections_layout_type_check;
    END IF;

    -- Drop existing section_type constraint if it exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'platform_landing_page_sections_section_type_check'
    ) THEN
        ALTER TABLE platform_landing_page_sections 
        DROP CONSTRAINT platform_landing_page_sections_section_type_check;
    END IF;
END $$;

-- Create new layout_type constraint with all enum values
ALTER TABLE platform_landing_page_sections
ADD CONSTRAINT platform_landing_page_sections_layout_type_check
CHECK (layout_type IN (
    'GRID',
    'ROW',
    'SCROLLING_ROW',
    'CAROUSEL',
    'MASONRY',
    'LIST',
    'SPLIT',
    'FULL_WIDTH',
    'CONTAINED'
));

-- Create new section_type constraint with all enum values (including new professional types)
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
    'IMAGE_WITH_TEXT',
    'SPLIT_CONTENT',
    'IMAGE_GALLERY',
    'VIDEO_SECTION',
    'CARD_SECTION',
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

