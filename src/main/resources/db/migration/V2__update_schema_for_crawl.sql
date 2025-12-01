-- V2: Update schema to support crawling and translation
-- ==========================================
-- EXTENSIONS
-- ==========================================
CREATE EXTENSION IF NOT EXISTS vector;

-- ==========================================
-- DROP OLD CONSTRAINTS AND COLUMNS
-- ==========================================

-- Drop foreign key constraint from stories to users
ALTER TABLE IF EXISTS stories DROP CONSTRAINT IF EXISTS fk_story_author;

-- Drop chapters table constraints and rename it
ALTER TABLE IF EXISTS chapters DROP CONSTRAINT IF EXISTS fk_chapter_story;
ALTER TABLE IF EXISTS chapters DROP CONSTRAINT IF EXISTS uk_story_chapter_number;

-- Drop reading_history constraints
ALTER TABLE IF EXISTS reading_history DROP CONSTRAINT IF EXISTS fk_history_user;
ALTER TABLE IF EXISTS reading_history DROP CONSTRAINT IF EXISTS fk_history_story;
ALTER TABLE IF EXISTS reading_history DROP CONSTRAINT IF EXISTS fk_history_chapter;
ALTER TABLE IF EXISTS reading_history DROP CONSTRAINT IF EXISTS uk_user_story;

-- ==========================================
-- UPDATE USERS TABLE
-- ==========================================
ALTER TABLE users DROP COLUMN IF EXISTS username;
ALTER TABLE users DROP COLUMN IF EXISTS updated_at;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);

-- Migrate existing data if any (only if password column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'users' AND column_name = 'password') THEN
        UPDATE users SET password_hash = password WHERE password_hash IS NULL;
    END IF;
END $$;

UPDATE users SET display_name = COALESCE(email, 'User') WHERE display_name IS NULL;

-- Drop old password column
ALTER TABLE users DROP COLUMN IF EXISTS password;

-- Make new columns not null (only if they have values)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE password_hash IS NULL) THEN
        ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM users WHERE display_name IS NULL) THEN
        ALTER TABLE users ALTER COLUMN display_name SET NOT NULL;
    END IF;
END $$;

-- ==========================================
-- UPDATE STORIES TABLE
-- ==========================================
ALTER TABLE stories DROP COLUMN IF EXISTS author_id;
ALTER TABLE stories DROP COLUMN IF EXISTS genre;
ALTER TABLE stories DROP COLUMN IF EXISTS status;
ALTER TABLE stories DROP COLUMN IF EXISTS updated_at;

ALTER TABLE stories ADD COLUMN IF NOT EXISTS author_name TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS cover_image_url TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS embedding VECTOR(1536);
ALTER TABLE stories ADD COLUMN IF NOT EXISTS source_url TEXT;
ALTER TABLE stories ADD COLUMN IF NOT EXISTS source_site VARCHAR(255) DEFAULT 'syosetu';

ALTER TABLE stories ALTER COLUMN title TYPE TEXT;

-- Create indexes for stories
DROP INDEX IF EXISTS idx_stories_author;
DROP INDEX IF EXISTS idx_stories_genre;
CREATE INDEX IF NOT EXISTS idx_stories_title ON stories USING GIN (to_tsvector('simple', title));
CREATE INDEX IF NOT EXISTS idx_stories_description ON stories USING GIN (to_tsvector('simple', description));

-- ==========================================
-- UPDATE CHAPTERS TABLE (RENAME TO story_chapters)
-- ==========================================
ALTER TABLE IF EXISTS chapters RENAME TO story_chapters;

ALTER TABLE story_chapters DROP COLUMN IF EXISTS updated_at;
ALTER TABLE story_chapters DROP COLUMN IF EXISTS content;
ALTER TABLE story_chapters DROP COLUMN IF EXISTS chapter_number;

ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS chapter_index INT;
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS raw_content TEXT;
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS crawl_status VARCHAR(50) DEFAULT 'PENDING';
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS crawl_time TIMESTAMP;
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS translated_content TEXT;
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS translate_status VARCHAR(50) DEFAULT 'NONE';
ALTER TABLE story_chapters ADD COLUMN IF NOT EXISTS translate_time TIMESTAMP;

ALTER TABLE story_chapters ALTER COLUMN title DROP NOT NULL;

-- Recreate foreign key
ALTER TABLE story_chapters ADD CONSTRAINT fk_chapter_story
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE;

-- Create indexes for chapters
DROP INDEX IF EXISTS idx_chapters_story;
CREATE INDEX IF NOT EXISTS idx_chapter_story ON story_chapters(story_id);
CREATE INDEX IF NOT EXISTS idx_chapter_index ON story_chapters(story_id, chapter_index);

-- ==========================================
-- UPDATE READING_HISTORY TABLE (RENAME TO user_history_read)
-- ==========================================
ALTER TABLE IF EXISTS reading_history RENAME TO user_history_read;

ALTER TABLE user_history_read DROP COLUMN IF EXISTS progress;
ALTER TABLE user_history_read DROP COLUMN IF EXISTS created_at;
ALTER TABLE user_history_read DROP COLUMN IF EXISTS updated_at;

ALTER TABLE user_history_read ADD COLUMN IF NOT EXISTS progress_percent INT DEFAULT 0;
ALTER TABLE user_history_read ADD COLUMN IF NOT EXISTS scroll_offset INT DEFAULT 0;

-- Recreate foreign keys
ALTER TABLE user_history_read ADD CONSTRAINT fk_history_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_history_read ADD CONSTRAINT fk_history_story
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE;
ALTER TABLE user_history_read ADD CONSTRAINT fk_history_chapter
    FOREIGN KEY (chapter_id) REFERENCES story_chapters(id) ON DELETE SET NULL;

-- Update indexes
DROP INDEX IF EXISTS idx_history_user;
DROP INDEX IF EXISTS idx_history_story;

-- ==========================================
-- CREATE CRAWL_JOBS TABLE
-- ==========================================
CREATE TABLE IF NOT EXISTS crawl_jobs (
    id SERIAL PRIMARY KEY,
    story_id INT REFERENCES stories(id) ON DELETE CASCADE,
    chapter_id INT REFERENCES story_chapters(id) ON DELETE CASCADE,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    attempts INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crawl_jobs_status ON crawl_jobs(status);
CREATE INDEX IF NOT EXISTS idx_crawl_jobs_story ON crawl_jobs(story_id);

