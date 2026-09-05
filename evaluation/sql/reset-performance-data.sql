SELECT set_config('m9.evaluation_db', :'evaluation_db', false);
DO $$
BEGIN
    IF current_database() <> current_setting('m9.evaluation_db')
       OR current_database() = 'crowd_feedback' THEN
        RAISE EXCEPTION 'M9 evaluation database safety guard failed';
    END IF;
END
$$;
DELETE FROM feedback_report WHERE feedback_id IN (SELECT feedback_id FROM feedback WHERE normalized_line='M9EVAL');
DELETE FROM gamification_event WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test');
DELETE FROM pinned_achievement WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test');
DELETE FROM user_achievement WHERE user_id IN (SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test');
DELETE FROM feedback WHERE normalized_line='M9EVAL';
DELETE FROM app_user WHERE email LIKE 'm9eval%@example.test';
