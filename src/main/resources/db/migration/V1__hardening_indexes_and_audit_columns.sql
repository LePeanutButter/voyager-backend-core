-- Production hardening migration: lightweight, idempotent index/audit updates.

ALTER TABLE IF EXISTS activities
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE IF EXISTS user_connections
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE IF EXISTS user_interests
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE IF EXISTS travel_plan_participants
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

DO $$
BEGIN
    IF to_regclass('public.shared_activities') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_shared_activities_sender_id
            ON shared_activities(sender_id);

        CREATE INDEX IF NOT EXISTS idx_shared_activities_receiver_id
            ON shared_activities(receiver_id);
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.travel_plans') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_travel_plans_destination_location
            ON travel_plans(destination_location);
    END IF;
END $$;
