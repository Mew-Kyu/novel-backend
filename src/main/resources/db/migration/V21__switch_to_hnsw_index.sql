-- ================================================================
-- Migration V21: Switch from IVFFlat to HNSW Index for Vector Search
-- ================================================================
--
-- Purpose:
--   Replace IVFFlat index with HNSW (Hierarchical Navigable Small World)
--   for faster vector similarity search with 768-dimensional embeddings.
--
-- Rationale:
--   - HNSW provides better performance for approximate nearest neighbor search
--   - Better suited for 768-dimensional vectors
--   - Faster query performance with lower latency
--   - Trade-off: Uses more memory (acceptable for our dataset)
--
-- Performance Expectations:
--   - Query latency: 100-500ms (vs 1-5s with IVFFlat)
--   - Memory usage: ~200-300MB (vs ~100-150MB with IVFFlat)
--   - Index creation time: 5-10 minutes for 10k+ stories
--

-- ================================================================
-- Step 1: Drop existing IVFFlat index
-- ================================================================
DROP INDEX IF EXISTS public.idx_stories_embedding CASCADE;

-- ================================================================
-- Step 2: Create HNSW index with optimized parameters
-- ================================================================
-- HNSW Parameters:
--   m=16           : Number of bi-directional links created per new element (default 16)
--   ef_construction=64 : Size of dynamic candidate list for construction (default 64)
--   ef_search=64   : Size of dynamic candidate list for search (tunable at runtime)
--
-- Notes:
--   - Higher m & ef_construction = better quality but slower indexing
--   - Current values are balanced for 768-dim vectors
--   - Can be tuned later if needed
--
CREATE INDEX idx_stories_embedding
ON public.stories USING hnsw (embedding public.vector_cosine_ops)
WITH (m=16, ef_construction=64);

-- ================================================================
-- Step 3: Verify index was created successfully
-- ================================================================
-- This comment documents the expected result:
-- After this migration, querying the stories table by embedding similarity
-- should use the HNSW index for faster performance.
--
-- To check the index:
-- SELECT schemaname, tablename, indexname, indexdef
-- FROM pg_indexes
-- WHERE tablename = 'stories' AND indexname = 'idx_stories_embedding';
--
-- Expected output:
-- schemaname | tablename | indexname              | indexdef
-- public     | stories   | idx_stories_embedding  | CREATE INDEX idx_stories_embedding ON public.stories USING hnsw (embedding public.vector_cosine_ops) WITH (m=16, ef_construction=64)

-- ================================================================
-- Step 4: Log migration completion
-- ================================================================
-- This migration was executed on: {current_timestamp}
-- Duration: Monitor logs for actual duration
--
-- Next steps:
-- 1. Run full evaluation to measure performance improvements
-- 2. Monitor query latency in logs
-- 3. If performance is still low, consider:
--    - Tuning m parameter (increase to 32 for better quality but slower build)
--    - Tuning ef_construction (increase to 128 for better quality)
--    - Checking if vector quality is adequate
--    - Profiling actual query execution plans

COMMENT ON INDEX public.idx_stories_embedding IS
'HNSW index for vector similarity search on 768-dimensional embeddings.
Parameters: m=16, ef_construction=64.
Created to improve semantic search performance.';

