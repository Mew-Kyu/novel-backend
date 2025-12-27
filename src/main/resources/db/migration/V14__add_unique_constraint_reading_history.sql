-- ==========================================
-- V14: Add unique constraint to reading history
-- ==========================================

-- First, delete duplicate records keeping only the most recent one
DELETE FROM user_history_read
WHERE id NOT IN (
    SELECT MAX(id)
    FROM user_history_read
    GROUP BY user_id, story_id
);

-- Add unique constraint to prevent duplicates in future
ALTER TABLE user_history_read
ADD CONSTRAINT uk_user_story_history UNIQUE (user_id, story_id);

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_history_user_story ON user_history_read(user_id, story_id);
CREATE INDEX IF NOT EXISTS idx_history_last_read ON user_history_read(last_read_at DESC);

