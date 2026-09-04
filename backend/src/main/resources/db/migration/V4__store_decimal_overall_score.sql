ALTER TABLE feedback
    ALTER COLUMN score TYPE DOUBLE PRECISION USING score::DOUBLE PRECISION;

UPDATE feedback
SET score = ROUND(
    ((punctuality_score + cleanliness_score + crowding_score)::NUMERIC / 3.0),
    1
)::DOUBLE PRECISION;
