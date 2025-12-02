-- Create genres table
CREATE TABLE IF NOT EXISTS genres (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create story_genres junction table for many-to-many relationship
CREATE TABLE IF NOT EXISTS story_genres (
    story_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (story_id, genre_id),
    CONSTRAINT fk_story_genres_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT fk_story_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

-- Create ratings table
CREATE TABLE IF NOT EXISTS ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_story_rating UNIQUE (user_id, story_id)
);

-- Create comments table
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_story FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_story_genres_story ON story_genres(story_id);
CREATE INDEX IF NOT EXISTS idx_story_genres_genre ON story_genres(genre_id);
CREATE INDEX IF NOT EXISTS idx_ratings_user ON ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_ratings_story ON ratings(story_id);
CREATE INDEX IF NOT EXISTS idx_ratings_story_created ON ratings(story_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_user ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_story ON comments(story_id);
CREATE INDEX IF NOT EXISTS idx_comments_story_created ON comments(story_id, created_at DESC);

-- Seed initial genre data
INSERT INTO genres (name, description) VALUES
    ('Isekai', 'Stories where the protagonist is transported to, reborn, or trapped in a parallel universe or fantasy world'),
    ('Romance', 'Stories focused on romantic relationships and love'),
    ('Comedy', 'Stories with humorous elements and comedic situations'),
    ('Fantasy', 'Stories set in magical or supernatural worlds'),
    ('Action', 'Stories with exciting action sequences and adventures'),
    ('Drama', 'Stories with serious themes and emotional depth'),
    ('Slice of Life', 'Stories depicting everyday life and ordinary events'),
    ('Adventure', 'Stories about exciting journeys and explorations'),
    ('Mystery', 'Stories involving puzzles, crimes, or secrets to be solved'),
    ('Horror', 'Stories designed to frighten or unsettle readers'),
    ('Sci-Fi', 'Stories based on science and technology, often set in the future'),
    ('Harem', 'Stories where one character is surrounded by multiple love interests'),
    ('School Life', 'Stories set in educational institutions'),
    ('Seinen', 'Stories targeted at young adult males'),
    ('Shounen', 'Stories targeted at young boys'),
    ('Shoujo', 'Stories targeted at young girls'),
    ('Josei', 'Stories targeted at adult women'),
    ('Historical', 'Stories set in historical periods'),
    ('Martial Arts', 'Stories featuring martial arts and combat skills'),
    ('Cultivation', 'Stories about characters training to gain supernatural powers')
ON CONFLICT (name) DO NOTHING;

-- Add comments to tables for documentation
COMMENT ON TABLE genres IS 'Available genres for categorizing stories';
COMMENT ON TABLE story_genres IS 'Many-to-many relationship between stories and genres';
COMMENT ON TABLE ratings IS 'User ratings for stories (1-5 stars)';
COMMENT ON TABLE comments IS 'User comments and reviews for stories';
COMMENT ON COLUMN ratings.rating IS 'Rating value from 1 to 5 stars';
COMMENT ON CONSTRAINT uk_user_story_rating ON ratings IS 'A user can only rate a story once (can be updated)';

