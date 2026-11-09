-- Payload documents are written and read as JSON text by the service layer.
-- Converting to TEXT keeps the exact JSON document while avoiding driver-level
-- PGobject coercion when the payload is produced by application code.
ALTER TABLE notifications ALTER COLUMN payload DROP DEFAULT;
ALTER TABLE notifications ALTER COLUMN payload TYPE TEXT USING payload::text;
ALTER TABLE notifications ALTER COLUMN payload SET DEFAULT '{}';

ALTER TABLE communication_events ALTER COLUMN payload TYPE TEXT USING payload::text;

ALTER TABLE moderation_audit_logs ALTER COLUMN details DROP DEFAULT;
ALTER TABLE moderation_audit_logs ALTER COLUMN details TYPE TEXT USING details::text;
ALTER TABLE moderation_audit_logs ALTER COLUMN details SET DEFAULT '{}';
