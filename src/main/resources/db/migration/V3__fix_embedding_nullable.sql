-- V3: Fix embedding column to allow NULL values
-- ==========================================

-- Make sure pgvector extension is enabled
CREATE EXTENSION IF NOT EXISTS vector;

-- Alter embedding column to explicitly allow NULL
ALTER TABLE stories ALTER COLUMN embedding DROP NOT NULL;

-- Add comment for clarity
COMMENT ON COLUMN stories.embedding IS 'Vector embedding for semantic search - can be NULL for newly crawled stories';

