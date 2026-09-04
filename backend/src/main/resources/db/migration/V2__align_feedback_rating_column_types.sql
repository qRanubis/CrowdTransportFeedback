ALTER TABLE feedback
    ALTER COLUMN score TYPE INTEGER USING score::INTEGER,
    ALTER COLUMN punctuality_score TYPE INTEGER USING punctuality_score::INTEGER,
    ALTER COLUMN cleanliness_score TYPE INTEGER USING cleanliness_score::INTEGER,
    ALTER COLUMN crowding_score TYPE INTEGER USING crowding_score::INTEGER;
