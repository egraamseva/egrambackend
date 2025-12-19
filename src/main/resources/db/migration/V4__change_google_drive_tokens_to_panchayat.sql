-- Migration to change google_drive_tokens from user-based to panchayat-based
-- V4__change_google_drive_tokens_to_panchayat.sql

-- Note: This migration will delete existing tokens as we're changing from user-based to panchayat-based tokens
-- Users will need to reconnect their Google Drive after this migration

DO $$
BEGIN
    -- Drop the old foreign key constraint
    IF EXISTS (SELECT 1 FROM pg_constraint 
               WHERE conname = 'fk_token_user') THEN
        ALTER TABLE google_drive_tokens DROP CONSTRAINT fk_token_user;
    END IF;
    
    -- Drop the old index
    IF EXISTS (SELECT 1 FROM pg_indexes 
               WHERE tablename = 'google_drive_tokens' AND indexname = 'idx_token_user') THEN
        DROP INDEX IF EXISTS idx_token_user;
    END IF;
    
    -- Delete all existing tokens (since we're changing from user-based to panchayat-based)
    -- Users will need to reconnect after this migration
    DELETE FROM google_drive_tokens;
    
    -- Rename user_id column to panchayat_id
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'google_drive_tokens' AND column_name = 'user_id') THEN
        ALTER TABLE google_drive_tokens RENAME COLUMN user_id TO panchayat_id;
    END IF;
    
    -- Add new foreign key constraint to panchayats table
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'fk_token_panchayat') THEN
        ALTER TABLE google_drive_tokens 
        ADD CONSTRAINT fk_token_panchayat 
        FOREIGN KEY (panchayat_id) REFERENCES panchayats(id) ON DELETE CASCADE;
    END IF;
    
    -- Create new index on panchayat_id
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'google_drive_tokens' AND indexname = 'idx_token_panchayat') THEN
        CREATE UNIQUE INDEX idx_token_panchayat ON google_drive_tokens(panchayat_id);
    END IF;
END $$;

