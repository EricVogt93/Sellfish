-- Weitere internationale Job-Quellen registrieren.
-- Keyless Quellen sind direkt aktiv; Quellen mit Pflicht-Key/Config bleiben deaktiviert,
-- bis Admin sie über /api/admin/job-sources konfiguriert.

INSERT INTO job_sources (id, code, name, enabled, config) VALUES
    (gen_random_uuid(), 'ARBEITNOW', 'Arbeitnow',           true,  '{}'::jsonb),
    (gen_random_uuid(), 'REMOTIVE',  'Remotive',            true,  '{}'::jsonb),
    (gen_random_uuid(), 'REMOTEOK',  'RemoteOK',            true,  '{}'::jsonb),
    (gen_random_uuid(), 'JOBICY',    'Jobicy',              true,  '{}'::jsonb),
    (gen_random_uuid(), 'HIMALAYAS', 'Himalayas',           true,  '{}'::jsonb),
    (gen_random_uuid(), 'THEMUSE',   'The Muse',            true,  '{}'::jsonb),
    (gen_random_uuid(), 'USAJOBS',   'USAJOBS (US)',        false, '{}'::jsonb),
    (gen_random_uuid(), 'REED',      'Reed (UK)',           false, '{}'::jsonb),
    (gen_random_uuid(), 'JOOBLE',    'Jooble',              false, '{}'::jsonb),
    (gen_random_uuid(), 'FINDWORK',  'Findwork',            false, '{}'::jsonb),
    (gen_random_uuid(), 'GREENHOUSE','Greenhouse (ATS)',    false, '{}'::jsonb),
    (gen_random_uuid(), 'LEVER',     'Lever (ATS)',         false, '{}'::jsonb)
ON CONFLICT (code) DO NOTHING;
