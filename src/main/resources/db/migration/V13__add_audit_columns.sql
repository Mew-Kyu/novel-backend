-- Add audit columns to stories, story_chapters, and crawl_jobs tables
-- This migration adds JPA auditing support for tracking creation and modification metadata

-- Add audit columns to stories table
ALTER TABLE stories
    ADD COLUMN created_by BIGINT,
    ADD COLUMN last_modified_by BIGINT;

-- Add foreign key constraints for stories audit columns
ALTER TABLE stories
    ADD CONSTRAINT fk_stories_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_stories_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add audit columns to story_chapters table
ALTER TABLE story_chapters
    ADD COLUMN created_by BIGINT,
    ADD COLUMN last_modified_by BIGINT;

-- Add foreign key constraints for story_chapters audit columns
ALTER TABLE story_chapters
    ADD CONSTRAINT fk_chapters_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_chapters_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add audit columns to crawl_jobs table
ALTER TABLE crawl_jobs
    ADD COLUMN created_by BIGINT,
    ADD COLUMN last_modified_by BIGINT;

-- Add foreign key constraints for crawl_jobs audit columns
ALTER TABLE crawl_jobs
    ADD CONSTRAINT fk_crawl_jobs_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_crawl_jobs_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id) ON DELETE SET NULL;

-- Note: The created_at and updated_at columns already exist in these tables
-- created_by and last_modified_by are nullable to support:
-- 1. Existing data (which has no creator information)
-- 2. System-generated data (background jobs running without authentication)
-- 3. Data cleanup if a user is deleted (ON DELETE SET NULL)

