-- Weitere Quellen: Working Nomads (keyless) und ATS-Boards (per Firma zu konfigurieren).

INSERT INTO job_sources (id, code, name, enabled, config) VALUES
    (gen_random_uuid(), 'WORKINGNOMADS',   'Working Nomads',          true,  '{}'::jsonb),
    (gen_random_uuid(), 'ASHBY',           'Ashby (ATS)',             false, '{}'::jsonb),
    (gen_random_uuid(), 'RECRUITEE',       'Recruitee (ATS)',         false, '{}'::jsonb),
    (gen_random_uuid(), 'SMARTRECRUITERS', 'SmartRecruiters (ATS)',   false, '{}'::jsonb)
ON CONFLICT (code) DO NOTHING;
