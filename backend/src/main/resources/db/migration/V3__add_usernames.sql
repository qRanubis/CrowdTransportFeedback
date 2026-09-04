ALTER TABLE app_user ADD COLUMN username VARCHAR(20);

WITH candidates AS (
    SELECT
        id,
        lower(regexp_replace(split_part(email, '@', 1), '[^a-z0-9]', '', 'g')) AS candidate,
        row_number() OVER (
            PARTITION BY lower(regexp_replace(split_part(email, '@', 1), '[^a-z0-9]', '', 'g'))
            ORDER BY id
        ) AS candidate_rank
    FROM app_user
)
UPDATE app_user AS u
SET username = CASE
    WHEN length(c.candidate) BETWEEN 3 AND 20 AND c.candidate_rank = 1
        THEN c.candidate
    ELSE 'user' || substring(replace(u.id::text, '-', '') FROM 1 FOR 16)
END
FROM candidates AS c
WHERE c.id = u.id;

ALTER TABLE app_user ALTER COLUMN username SET NOT NULL;
ALTER TABLE app_user ADD CONSTRAINT uk_user_username UNIQUE (username);
ALTER TABLE app_user ADD CONSTRAINT ck_user_username CHECK (username ~ '^[a-z0-9]{3,20}$');
