-- Manual SQL script to update CHECK constraints
-- Run this directly on your PostgreSQL database if Flyway migration doesn't run automatically
-- This is a backup option - the Flyway migration V1__update_section_constraints.sql should run automatically

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

-- Create new section_type constraint with all enum values
ALTER TABLE panchayat_website_sections
ADD CONSTRAINT panchayat_website_sections_section_type_check
CHECK (section_type IN (
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
    'MAP'
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

-- Create new section_type constraint with all enum values
ALTER TABLE platform_landing_page_sections
ADD CONSTRAINT platform_landing_page_sections_section_type_check
CHECK (section_type IN (
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
    'MAP'
));

