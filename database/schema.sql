-- PostgreSQL schema for the DSA Hub application.
-- Import pipeline data with scripts/import_database.py; do not seed problems here.

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_uq ON users (lower(email));

CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    sync_progress_across_sheets BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION dsa_hub_create_default_user_settings()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO user_settings (user_id, sync_progress_across_sheets)
    VALUES (NEW.user_id, TRUE)
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS users_create_default_settings ON users;
CREATE TRIGGER users_create_default_settings
AFTER INSERT ON users
FOR EACH ROW EXECUTE FUNCTION dsa_hub_create_default_user_settings();

CREATE TABLE IF NOT EXISTS problems (
    problem_id TEXT PRIMARY KEY,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    leetcode_id TEXT UNIQUE,
    leetcode_slug TEXT,
    difficulty TEXT CHECK (difficulty IS NULL OR difficulty IN ('Easy', 'Medium', 'Hard')),
    leetcode_paid_only BOOLEAN,
    leetcode_has_solution BOOLEAN,
    leetcode_has_video_solution BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (leetcode_id IS NULL OR problem_id = 'lc-' || leetcode_id),
    CHECK (leetcode_slug IS NULL OR leetcode_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS problems_leetcode_slug_uq
    ON problems (leetcode_slug) WHERE leetcode_slug IS NOT NULL;
CREATE INDEX IF NOT EXISTS problems_title_lower_idx ON problems (lower(title));
CREATE INDEX IF NOT EXISTS problems_difficulty_idx ON problems (difficulty);

CREATE TABLE IF NOT EXISTS problem_links (
    problem_id TEXT NOT NULL REFERENCES problems(problem_id) ON DELETE CASCADE,
    link_type TEXT NOT NULL,
    url TEXT NOT NULL CHECK (url ~ '^https?://[^[:space:]]+$'),
    PRIMARY KEY (problem_id, link_type)
);
CREATE INDEX IF NOT EXISTS problem_links_type_idx ON problem_links (link_type);

CREATE TABLE IF NOT EXISTS problem_topics (
    problem_id TEXT NOT NULL REFERENCES problems(problem_id) ON DELETE CASCADE,
    topic TEXT NOT NULL CHECK (length(trim(topic)) > 0),
    PRIMARY KEY (problem_id, topic)
);
CREATE INDEX IF NOT EXISTS problem_topics_topic_idx ON problem_topics (topic, problem_id);

CREATE TABLE IF NOT EXISTS sheets (
    sheet_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 9999,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS sheets_enabled_order_idx
    ON sheets (display_order, sheet_id) WHERE is_enabled = TRUE;

-- One row per distinct sheet/problem pair. Repeated placements live below.
CREATE TABLE IF NOT EXISTS sheet_problems (
    sheet_id TEXT NOT NULL REFERENCES sheets(sheet_id) ON DELETE RESTRICT,
    problem_id TEXT NOT NULL REFERENCES problems(problem_id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (sheet_id, problem_id)
);
CREATE INDEX IF NOT EXISTS sheet_problems_problem_idx
    ON sheet_problems (problem_id, sheet_id) WHERE is_active = TRUE;

-- One row per position/section occurrence from the processed membership array.
CREATE TABLE IF NOT EXISTS sheet_problem_occurrences (
    occurrence_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sheet_id TEXT NOT NULL,
    problem_id TEXT NOT NULL,
    position INTEGER CHECK (position IS NULL OR position > 0),
    section TEXT,
    subsection TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (sheet_id, problem_id)
        REFERENCES sheet_problems(sheet_id, problem_id) ON DELETE RESTRICT,
    CONSTRAINT sheet_problem_occurrence_identity_uq
        UNIQUE NULLS NOT DISTINCT (sheet_id, problem_id, position, section, subsection)
);
CREATE INDEX IF NOT EXISTS sheet_occurrences_order_idx
    ON sheet_problem_occurrences (sheet_id, position, occurrence_id) WHERE is_active = TRUE;

CREATE TABLE IF NOT EXISTS user_problem_progress (
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    problem_id TEXT NOT NULL REFERENCES problems(problem_id) ON DELETE RESTRICT,
    solved BOOLEAN NOT NULL DEFAULT FALSE,
    solved_at TIMESTAMPTZ,
    notes TEXT,
    is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    revision_status TEXT CHECK (revision_status IS NULL OR revision_status IN ('new', 'learning', 'review', 'mastered')),
    user_difficulty TEXT CHECK (user_difficulty IS NULL OR user_difficulty IN ('Easy', 'Medium', 'Hard')),
    last_attempted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, problem_id),
    CHECK (NOT solved OR solved_at IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS user_problem_progress_solved_idx
    ON user_problem_progress (user_id, solved, problem_id);
CREATE INDEX IF NOT EXISTS user_problem_progress_bookmarked_idx
    ON user_problem_progress (user_id, problem_id) WHERE is_bookmarked = TRUE;

CREATE TABLE IF NOT EXISTS user_sheet_problem_progress (
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    sheet_id TEXT NOT NULL,
    problem_id TEXT NOT NULL,
    solved BOOLEAN NOT NULL DEFAULT FALSE,
    solved_at TIMESTAMPTZ,
    notes TEXT,
    is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    revision_status TEXT CHECK (revision_status IS NULL OR revision_status IN ('new', 'learning', 'review', 'mastered')),
    user_difficulty TEXT CHECK (user_difficulty IS NULL OR user_difficulty IN ('Easy', 'Medium', 'Hard')),
    last_attempted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, sheet_id, problem_id),
    FOREIGN KEY (sheet_id, problem_id)
        REFERENCES sheet_problems(sheet_id, problem_id) ON DELETE RESTRICT,
    CHECK (NOT solved OR solved_at IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS user_sheet_progress_sheet_idx
    ON user_sheet_problem_progress (user_id, sheet_id, solved, problem_id);

-- Attempt history is separate from the current progress summary.
CREATE TABLE IF NOT EXISTS user_problem_attempts (
    attempt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    problem_id TEXT NOT NULL REFERENCES problems(problem_id) ON DELETE RESTRICT,
    sheet_id TEXT,
    coding_platform TEXT,
    outcome TEXT NOT NULL CHECK (outcome IN ('attempted', 'solved', 'failed')),
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes TEXT,
    FOREIGN KEY (sheet_id, problem_id)
        REFERENCES sheet_problems(sheet_id, problem_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS user_attempts_history_idx
    ON user_problem_attempts (user_id, problem_id, attempted_at DESC);
