-- Add user profile table with embedding and temporal features

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,

    -- Aggregated embedding from read stories (weighted average)
    profile_embedding vector(768),

    -- Reading behavior metrics
    total_stories_read INTEGER DEFAULT 0,
    total_chapters_read INTEGER DEFAULT 0,
    average_completion_rate DECIMAL(5,2) DEFAULT 0.0, -- Percentage of stories completed

    -- Reading velocity (engagement metrics)
    chapters_per_week DECIMAL(10,2) DEFAULT 0.0,
    avg_session_duration_minutes DECIMAL(10,2) DEFAULT 0.0,

    -- Genre diversity
    genre_diversity_score DECIMAL(5,2) DEFAULT 0.0, -- 0-1: How diverse user's taste is

    -- Temporal features
    last_profile_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_embedding ON user_profiles USING ivfflat (profile_embedding vector_cosine_ops);
CREATE INDEX idx_user_profiles_last_update ON user_profiles(last_profile_update);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_user_profile_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update timestamp
CREATE TRIGGER trigger_update_user_profile_timestamp
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_user_profile_timestamp();

-- Comments for documentation
COMMENT ON TABLE user_profiles IS 'Stores aggregated user behavior and preferences for enhanced recommendations';
COMMENT ON COLUMN user_profiles.profile_embedding IS 'Weighted average of embeddings from stories user has interacted with';
COMMENT ON COLUMN user_profiles.average_completion_rate IS 'Percentage of started stories that user completed (progress >= 90%)';
COMMENT ON COLUMN user_profiles.genre_diversity_score IS 'Measures how diverse users reading taste is (0=narrow, 1=very diverse)';
COMMENT ON COLUMN user_profiles.chapters_per_week IS 'Average reading velocity in recent 30 days';
-- V19__add_user_profile_table.sql

