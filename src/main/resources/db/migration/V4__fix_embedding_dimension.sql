-- V4: Fix embedding vector dimension from 1536 to 768
-- ==========================================
-- Gemini's text-embedding-004 model produces 768-dimensional embeddings
-- We need to update the column to match

-- Drop existing embedding column and recreate with correct dimensions
ALTER TABLE stories DROP COLUMN IF EXISTS embedding;

-- Add embedding column with correct dimension (768 for text-embedding-004)
ALTER TABLE stories ADD COLUMN embedding VECTOR(768);

-- Add index for similarity search
CREATE INDEX IF NOT EXISTS idx_stories_embedding ON stories USING ivfflat (embedding vector_cosine_ops);

-- Add comment
COMMENT ON COLUMN stories.embedding IS 'Vector embedding (768 dimensions) from Gemini text-embedding-004 for semantic search';

