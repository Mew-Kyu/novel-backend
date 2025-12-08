-- Create favorites table for users to bookmark their favorite stories
CREATE TABLE IF NOT EXISTS favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_story_favorite UNIQUE (user_id, story_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_story ON favorites(story_id);
CREATE INDEX IF NOT EXISTS idx_favorites_created ON favorites(user_id, created_at DESC);

-- Add comment for documentation
COMMENT ON TABLE favorites IS 'User favorite/bookmarked stories';
COMMENT ON CONSTRAINT uk_user_story_favorite ON favorites IS 'A user can only favorite a story once';

