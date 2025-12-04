-- Migration to add theme_id column to panchayats table
-- This allows panchayats to have a custom theme for their website

ALTER TABLE panchayats
ADD COLUMN IF NOT EXISTS theme_id VARCHAR(50);

-- Add index for theme_id lookups
CREATE INDEX IF NOT EXISTS idx_panchayat_theme_id ON panchayats(theme_id);

