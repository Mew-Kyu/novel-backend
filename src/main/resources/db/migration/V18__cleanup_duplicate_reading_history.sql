-- ==========================================
-- V18: Clean up any duplicate reading history records
-- ==========================================

-- This migration handles any duplicates that may have been created due to race conditions
-- Keep only the most recent record for each user-story combination

-- First, identify and delete duplicate records, keeping only the most recent one
DELETE FROM user_history_read
WHERE id NOT IN (
    SELECT MAX(id)
    FROM user_history_read
    GROUP BY user_id, story_id
);

-- Verify the unique constraint exists (it should from V14, but ensure it's there)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_user_story_history'
    ) THEN
        ALTER TABLE user_history_read
        ADD CONSTRAINT uk_user_story_history UNIQUE (user_id, story_id);
    END IF;
END $$;

-- Add comment for documentation
COMMENT ON CONSTRAINT uk_user_story_history ON user_history_read IS
    'Ensures each user can only have one reading history record per story';

