-- Add raw author name column to stories table
ALTER TABLE stories ADD COLUMN IF NOT EXISTS raw_author_name TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS translated_author_name TEXT;

-- Add raw title column to story_chapters table
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS raw_title TEXT;
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS translated_title TEXT;

-- Migrate existing chapter titles to raw_title column
UPDATE story_chapters
SET raw_title = title
WHERE raw_title IS NULL AND title IS NOT NULL;

-- Add comments for clarity
COMMENT ON COLUMN stories.raw_author_name IS 'Original author name from crawled source (Japanese)';
COMMENT ON COLUMN stories.translated_author_name IS 'Translated author name (Vietnamese)';
COMMENT ON COLUMN story_chapters.raw_title IS 'Original chapter title from crawled source (Japanese)';
COMMENT ON COLUMN story_chapters.translated_title IS 'Translated chapter title (Vietnamese)';

