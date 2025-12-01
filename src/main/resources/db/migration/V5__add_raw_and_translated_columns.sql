-- Add raw and translated columns for title and description
ALTER TABLE stories ADD COLUMN IF NOT EXISTS raw_title TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS raw_description TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS translated_title TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS translated_description TEXT;

-- Migrate existing data: move current title/description to raw columns
UPDATE stories
SET raw_title = title,
    raw_description = description
WHERE raw_title IS NULL;

-- Add comment to clarify column usage
COMMENT ON COLUMN stories.raw_title IS 'Original title from crawled source (Japanese)';
COMMENT ON COLUMN stories.raw_description IS 'Original description from crawled source (Japanese)';
COMMENT ON COLUMN stories.translated_title IS 'Translated title (Vietnamese)';
COMMENT ON COLUMN stories.translated_description IS 'Translated description (Vietnamese)';

