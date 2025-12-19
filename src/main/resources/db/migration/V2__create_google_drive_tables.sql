-- Migration to create Google Drive integration tables
-- V2__create_google_drive_tables.sql

-- Create user_consents table
CREATE TABLE IF NOT EXISTS user_consents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    consent_given BOOLEAN NOT NULL DEFAULT false,
    consent_timestamp TIMESTAMP NOT NULL,
    purpose VARCHAR(500) DEFAULT 'Google Drive document storage for Panchayat documents',
    ip_address VARCHAR(45),
    revoked_at TIMESTAMP,
    revoke_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for user_consents
CREATE INDEX IF NOT EXISTS idx_consent_user ON user_consents(user_id);
CREATE INDEX IF NOT EXISTS idx_consent_active ON user_consents(user_id, revoked_at);

-- Create google_drive_tokens table
CREATE TABLE IF NOT EXISTS google_drive_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expiry_time TIMESTAMP NOT NULL,
    scope VARCHAR(500) DEFAULT 'https://www.googleapis.com/auth/drive.file',
    token_type VARCHAR(50) DEFAULT 'Bearer',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for google_drive_tokens
CREATE INDEX IF NOT EXISTS idx_token_user ON google_drive_tokens(user_id);

-- Update documents table to add Google Drive fields
DO $$
BEGIN
    -- Add google_drive_file_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'documents' AND column_name = 'google_drive_file_id') THEN
        ALTER TABLE documents ADD COLUMN google_drive_file_id VARCHAR(255);
    END IF;
    
    -- Add file_name column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'documents' AND column_name = 'file_name') THEN
        ALTER TABLE documents ADD COLUMN file_name VARCHAR(500);
    END IF;
    
    -- Add visibility column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'documents' AND column_name = 'visibility') THEN
        ALTER TABLE documents ADD COLUMN visibility VARCHAR(20) DEFAULT 'PRIVATE';
    END IF;
    
    -- Add consent_id column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'documents' AND column_name = 'consent_id') THEN
        ALTER TABLE documents ADD COLUMN consent_id BIGINT;
    END IF;
    
    -- Add is_available column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'documents' AND column_name = 'is_available') THEN
        ALTER TABLE documents ADD COLUMN is_available BOOLEAN DEFAULT true;
    END IF;
END $$;

-- Create unique index for google_drive_file_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_document_drive_id ON documents(google_drive_file_id) 
WHERE google_drive_file_id IS NOT NULL;

-- Create index for visibility
CREATE INDEX IF NOT EXISTS idx_document_visibility ON documents(visibility);

-- Add foreign key for consent
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'fk_document_consent') THEN
        ALTER TABLE documents 
        ADD CONSTRAINT fk_document_consent 
        FOREIGN KEY (consent_id) REFERENCES user_consents(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Make file_url nullable (since we're using Google Drive now)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'documents' AND column_name = 'file_url' AND is_nullable = 'NO') THEN
        ALTER TABLE documents ALTER COLUMN file_url DROP NOT NULL;
    END IF;
END $$;

-- Add check constraint for visibility
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'documents_visibility_check') THEN
        ALTER TABLE documents 
        ADD CONSTRAINT documents_visibility_check 
        CHECK (visibility IN ('PUBLIC', 'PRIVATE'));
    END IF;
END $$;

-- Update existing documents to have default visibility
UPDATE documents 
SET visibility = 'PRIVATE' 
WHERE visibility IS NULL;

