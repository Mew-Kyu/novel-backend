-- Add average_rating and total_ratings columns to stories table for caching
ALTER TABLE stories ADD COLUMN average_rating NUMERIC(3, 1) DEFAULT NULL;
ALTER TABLE stories ADD COLUMN total_ratings BIGINT DEFAULT 0;

-- Update existing stories with calculated values
UPDATE stories s SET
    average_rating = ROUND(COALESCE((SELECT AVG(rating) FROM ratings WHERE story_id = s.id), 0)::NUMERIC, 1),
    total_ratings = COALESCE((SELECT COUNT(*) FROM ratings WHERE story_id = s.id), 0);

