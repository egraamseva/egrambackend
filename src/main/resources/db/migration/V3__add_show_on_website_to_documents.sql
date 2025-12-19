-- Migration to add show_on_website field to documents table
-- V3__add_show_on_website_to_documents.sql

-- Add show_on_website column
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'documents' AND column_name = 'show_on_website') THEN
        ALTER TABLE documents ADD COLUMN show_on_website BOOLEAN NOT NULL DEFAULT false;
    END IF;
END $$;

-- Create index for show_on_website
CREATE INDEX IF NOT EXISTS idx_document_show_on_website ON documents(show_on_website);

-- Update existing documents: if visibility is PUBLIC, set show_on_website to false by default
-- (Admin needs to explicitly enable it)
UPDATE documents 
SET show_on_website = false 
WHERE show_on_website IS NULL;

