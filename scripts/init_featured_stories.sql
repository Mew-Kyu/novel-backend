-- Initial Featured Stories Setup
-- This script marks top performing stories as featured
-- Run this ONCE after deploying the auto-update feature

-- Option 1: Mark top 10 stories by view count
UPDATE stories
SET featured = true
WHERE id IN (
    SELECT id FROM stories
    WHERE view_count > 0
    ORDER BY view_count DESC
    LIMIT 10
);

-- Option 2: Mark top stories by combined score (view + rating)
-- Uncomment if you prefer this approach
/*
UPDATE stories
SET featured = true
WHERE id IN (
    SELECT id
    FROM stories
    WHERE view_count >= 100
      AND updated_at >= NOW() - INTERVAL '30 days'
    ORDER BY
        (view_count / 1000.0) * 0.4 +
        COALESCE((average_rating * LOG(total_ratings + 1)) * 0.3, 0) +
        (EXP(-EXTRACT(DAY FROM (NOW() - updated_at)) / 7.0) * 10) * 0.3
    DESC
    LIMIT 10
);
*/

-- Verify results
SELECT
    id,
    title,
    view_count,
    average_rating,
    total_ratings,
    updated_at,
    featured
FROM stories
WHERE featured = true
ORDER BY view_count DESC;

-- Total count of featured stories
SELECT COUNT(*) as featured_count FROM stories WHERE featured = true;

