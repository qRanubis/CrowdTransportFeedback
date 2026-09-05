BEGIN;
SELECT set_config('m9.evaluation_db', :'evaluation_db', false);
DO $$
BEGIN
    IF current_database() <> current_setting('m9.evaluation_db')
       OR current_database() = 'crowd_feedback' THEN
        RAISE EXCEPTION 'M9 evaluation database safety guard failed';
    END IF;
END
$$;
CREATE TEMP TABLE m9_cleanup_user_ids ON COMMIT DROP AS
SELECT id FROM app_user WHERE email LIKE 'm9eval%@example.test';
CREATE TEMP TABLE m9_cleanup_feedback_ids ON COMMIT DROP AS
SELECT feedback_id FROM feedback
WHERE normalized_line = 'M9EVAL'
   OR created_by_user_id IN (SELECT id FROM m9_cleanup_user_ids);

DELETE FROM feedback_report
WHERE feedback_id IN (SELECT feedback_id FROM m9_cleanup_feedback_ids)
   OR reporter_user_id IN (SELECT id FROM m9_cleanup_user_ids)
   OR resolved_by_user_id IN (SELECT id FROM m9_cleanup_user_ids);
DELETE FROM admin_audit_log
WHERE admin_user_id IN (SELECT id FROM m9_cleanup_user_ids)
   OR target_id IN (SELECT feedback_id FROM m9_cleanup_feedback_ids);
DELETE FROM pinned_achievement WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids);
DELETE FROM user_achievement WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids);
DELETE FROM gamification_event WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids);
DELETE FROM refresh_session WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids);
DELETE FROM feedback WHERE feedback_id IN (SELECT feedback_id FROM m9_cleanup_feedback_ids);
DELETE FROM app_user WHERE id IN (SELECT id FROM m9_cleanup_user_ids);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM feedback WHERE normalized_line = 'M9EVAL')
       OR EXISTS (SELECT 1 FROM app_user WHERE email LIKE 'm9eval%@example.test')
       OR EXISTS (SELECT 1 FROM gamification_event WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids))
       OR EXISTS (SELECT 1 FROM pinned_achievement WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids))
       OR EXISTS (SELECT 1 FROM user_achievement WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids))
       OR EXISTS (SELECT 1 FROM refresh_session WHERE user_id IN (SELECT id FROM m9_cleanup_user_ids))
       OR EXISTS (SELECT 1 FROM feedback_report WHERE reporter_user_id IN (SELECT id FROM m9_cleanup_user_ids) OR resolved_by_user_id IN (SELECT id FROM m9_cleanup_user_ids) OR feedback_id IN (SELECT feedback_id FROM m9_cleanup_feedback_ids))
       OR EXISTS (SELECT 1 FROM admin_audit_log WHERE admin_user_id IN (SELECT id FROM m9_cleanup_user_ids) OR target_id IN (SELECT feedback_id FROM m9_cleanup_feedback_ids)) THEN
        RAISE EXCEPTION 'M9 evaluation reset postcondition failed';
    END IF;
END
$$;
COMMIT;
