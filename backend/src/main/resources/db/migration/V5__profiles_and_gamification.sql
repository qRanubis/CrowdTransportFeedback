ALTER TABLE app_user ADD COLUMN avatar_key VARCHAR(24) NOT NULL DEFAULT 'COMMUTER'
  CHECK (avatar_key IN ('COMMUTER','NAVIGATOR','EXPLORER'));
ALTER TABLE feedback ADD COLUMN normalized_line VARCHAR(32);
UPDATE feedback SET normalized_line = upper(regexp_replace(btrim(line), '\\s+', ' ', 'g'));
ALTER TABLE feedback ALTER COLUMN normalized_line SET NOT NULL;

CREATE TABLE gamification_event (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES app_user(id),
 event_type VARCHAR(40) NOT NULL, source_key VARCHAR(160) NOT NULL,
 xp_delta INTEGER NOT NULL, line_identity VARCHAR(80), created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_gamification_event UNIQUE(user_id,event_type,source_key)
);
CREATE INDEX idx_gamification_user_time ON gamification_event(user_id,created_at);
CREATE INDEX idx_gamification_cooldown ON gamification_event(user_id,line_identity,created_at);

CREATE TABLE user_achievement (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES app_user(id),
 achievement_code VARCHAR(48) NOT NULL, unlocked_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_user_achievement UNIQUE(user_id,achievement_code)
);
CREATE INDEX idx_achievement_user_time ON user_achievement(user_id,unlocked_at);
CREATE TABLE pinned_achievement (
 user_id UUID NOT NULL REFERENCES app_user(id), achievement_code VARCHAR(48) NOT NULL,
 display_order SMALLINT NOT NULL CHECK(display_order BETWEEN 0 AND 2),
 PRIMARY KEY(user_id,achievement_code), UNIQUE(user_id,display_order),
 FOREIGN KEY(user_id,achievement_code) REFERENCES user_achievement(user_id,achievement_code)
);

-- Idempotent historical XP backfill. Original feedback times are retained for monthly ranking.
INSERT INTO gamification_event(id,user_id,event_type,source_key,xp_delta,line_identity,created_at)
SELECT gen_random_uuid(),created_by_user_id,'FEEDBACK_BASE_AWARDED',feedback_id::text,10,
 transport_type||':'||normalized_line,to_timestamp(created_at/1000.0) FROM feedback ON CONFLICT DO NOTHING;
INSERT INTO gamification_event(id,user_id,event_type,source_key,xp_delta,created_at)
SELECT gen_random_uuid(),created_by_user_id,'FIRST_CONTRIBUTION_BONUS','LIFETIME',40,to_timestamp(min(created_at)/1000.0)
 FROM feedback GROUP BY created_by_user_id ON CONFLICT DO NOTHING;
INSERT INTO gamification_event(id,user_id,event_type,source_key,xp_delta,created_at)
SELECT gen_random_uuid(),created_by_user_id,'NEW_TRANSPORT_TYPE_BONUS',transport_type,40,to_timestamp(min(created_at)/1000.0)
 FROM feedback GROUP BY created_by_user_id,transport_type ON CONFLICT DO NOTHING;
INSERT INTO gamification_event(id,user_id,event_type,source_key,xp_delta,created_at)
SELECT gen_random_uuid(),created_by_user_id,'NEW_LINE_BONUS',transport_type||':'||normalized_line,30,to_timestamp(min(created_at)/1000.0)
 FROM feedback GROUP BY created_by_user_id,transport_type,normalized_line ON CONFLICT DO NOTHING;
