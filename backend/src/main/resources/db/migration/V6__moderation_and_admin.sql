CREATE TABLE feedback_report (
 id UUID PRIMARY KEY,
 feedback_id UUID NOT NULL,
 reporter_user_id UUID NOT NULL REFERENCES app_user(id),
 reason VARCHAR(40) NOT NULL CHECK (reason IN ('SPAM','FAKE_OR_MISLEADING','ABUSIVE_OR_INAPPROPRIATE','IRRELEVANT','DUPLICATE','OTHER')),
 details VARCHAR(250),
 status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','DISMISSED','CONFIRMED','CLOSED')),
 created_at TIMESTAMPTZ NOT NULL,
 resolved_at TIMESTAMPTZ,
 resolved_by_user_id UUID REFERENCES app_user(id),
 CONSTRAINT uk_feedback_reporter UNIQUE(feedback_id, reporter_user_id)
);
CREATE INDEX idx_report_feedback ON feedback_report(feedback_id);
CREATE INDEX idx_report_reporter ON feedback_report(reporter_user_id);
CREATE INDEX idx_report_status_created ON feedback_report(status, created_at DESC);

CREATE TABLE admin_audit_log (
 id UUID PRIMARY KEY,
 admin_user_id UUID NOT NULL REFERENCES app_user(id),
 action VARCHAR(40) NOT NULL CHECK (action IN ('REPORTS_DISMISSED','REPORTED_FEEDBACK_DELETED','FEEDBACK_DELETED')),
 target_type VARCHAR(32) NOT NULL,
 target_id UUID NOT NULL,
 details VARCHAR(1000),
 created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_admin_audit_created ON admin_audit_log(created_at DESC);
