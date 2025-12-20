-- Add status column to stories table
ALTER TABLE stories ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PUBLISHED';

-- Create index on status for efficient filtering
CREATE INDEX IF NOT EXISTS idx_stories_status ON stories(status);

-- Create index on featured and status for efficient filtering
CREATE INDEX IF NOT EXISTS idx_stories_featured_status ON stories(featured, status);

-- Update existing records to have PUBLISHED status
UPDATE stories SET status = 'PUBLISHED' WHERE status IS NULL;

