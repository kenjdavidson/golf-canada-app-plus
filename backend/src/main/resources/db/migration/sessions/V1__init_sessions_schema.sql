CREATE TABLE user_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Keep this limit aligned with the username length enforced in application code.
    username TEXT NOT NULL UNIQUE CHECK (length(username) <= 255),
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TEXT NOT NULL,
    -- SQLite represents booleans as INTEGER 0/1 values.
    remember_me INTEGER NOT NULL DEFAULT 0
);
