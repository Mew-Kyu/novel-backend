-- V10: Add Homepage Features (updatedAt, viewCount, featured)
-- Date: 2025-12-10

-- Add updatedAt to stories table
ALTER TABLE stories ADD COLUMN updated_at TIMESTAMP;
UPDATE stories SET updated_at = created_at WHERE updated_at IS NULL;

-- Add viewCount to stories table
ALTER TABLE stories ADD COLUMN view_count BIGINT DEFAULT 0;

-- Add featured flag to stories table
ALTER TABLE stories ADD COLUMN featured BOOLEAN DEFAULT FALSE;

-- Add updatedAt to chapters table
ALTER TABLE story_chapters ADD COLUMN updated_at TIMESTAMP;
UPDATE story_chapters SET updated_at = created_at WHERE updated_at IS NULL;

-- Create index for performance optimization
CREATE INDEX idx_stories_updated_at ON stories(updated_at DESC);
CREATE INDEX idx_stories_view_count ON stories(view_count DESC);
CREATE INDEX idx_stories_featured ON stories(featured) WHERE featured = true;
CREATE INDEX idx_chapters_updated_at ON story_chapters(updated_at DESC);

-- Comments for documentation
COMMENT ON COLUMN stories.updated_at IS 'Last update time of the story';
COMMENT ON COLUMN stories.view_count IS 'Total view count for the story';
COMMENT ON COLUMN stories.featured IS 'Whether the story is featured on homepage';
COMMENT ON COLUMN story_chapters.updated_at IS 'Last update time of the chapter';

