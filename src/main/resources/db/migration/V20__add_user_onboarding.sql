-- Add user onboarding table
-- V20__add_user_onboarding.sql

CREATE TABLE IF NOT EXISTS user_onboarding (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,

    -- Preferences collected during onboarding
    preferred_genres TEXT,
    reading_frequency VARCHAR(50),
    preferred_length VARCHAR(50),
    completion_preference VARCHAR(50),
    exploration_preference VARCHAR(50),

    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_onboarding_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_onboarding_user_id ON user_onboarding(user_id);
CREATE INDEX idx_onboarding_completed ON user_onboarding(completed);

COMMENT ON TABLE user_onboarding IS 'User preferences collected during first-time onboarding';
COMMENT ON COLUMN user_onboarding.preferred_genres IS 'Comma-separated list of preferred genre IDs';
COMMENT ON COLUMN user_onboarding.reading_frequency IS 'How often user reads: DAILY, WEEKLY, MONTHLY, CASUAL';
COMMENT ON COLUMN user_onboarding.exploration_preference IS 'SAFE (known genres), ADVENTUROUS (try new), BALANCED';

