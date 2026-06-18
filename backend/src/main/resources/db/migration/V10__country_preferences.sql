-- Länder-Präferenzen für Job-Quellen-Filter

ALTER TABLE user_preferences
    ADD COLUMN preferred_countries TEXT[] NOT NULL DEFAULT '{}';
