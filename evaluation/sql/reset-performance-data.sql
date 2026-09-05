SELECT CASE WHEN current_database() = :'evaluation_db' AND current_database() <> 'crowd_feedback' THEN 1 ELSE 1/0 END AS evaluation_database_guard;
DELETE FROM feedback_report WHERE feedback_id IN (SELECT feedback_id FROM feedback WHERE normalized_line='M9EVAL');
DELETE FROM gamification_event WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test');
DELETE FROM pinned_achievement WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test');
DELETE FROM user_achievement WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test');
DELETE FROM feedback WHERE normalized_line='M9EVAL';
DELETE FROM app_user WHERE email LIKE 'm9eval%@example.test';
