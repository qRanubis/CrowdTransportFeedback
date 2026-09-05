SELECT CASE WHEN current_database() = 'crowd_feedback_eval' THEN 1 ELSE 1/0 END AS evaluation_database_guard;
INSERT INTO app_user(id,email,username,password_hash,role,created_at,avatar_key)
SELECT ('00000000-0000-4000-8000-' || lpad(i::text,12,'0'))::uuid,
       'm9eval'||i||'@example.test','m9user'||lpad(i::text,3,'0'),'not-a-login-hash','USER',TIMESTAMPTZ '2026-01-01 00:00:00+00','COMMUTER'
FROM generate_series(1,100) i;
INSERT INTO feedback(feedback_id,created_by_user_id,transport_type,line,normalized_line,score,punctuality_score,cleanliness_score,crowding_score,comment,latitude,longitude,created_at)
SELECT ('10000000-0000-4000-8000-' || lpad(i::text,12,'0'))::uuid,
 ('00000000-0000-4000-8000-' || lpad((((i-1)%100)+1)::text,12,'0'))::uuid,
 'METRO','M9EVAL','M9EVAL',((i-1)%5)+1,((i-1)%5)+1,((i+1)%5)+1,((i+3)%5)+1,'M9 deterministic evaluation row',
 44.4268 + ((i-1)%5)*0.002,26.1025 + ((i-1)%4)*0.003,
 1768478400000 - ((i-1)%168)*3600000
FROM generate_series(1,:row_count) i;
SELECT CASE WHEN count(*) = :row_count THEN count(*) ELSE 1/0 END AS verified_feedback_count FROM feedback WHERE normalized_line='M9EVAL';
