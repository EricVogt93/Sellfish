-- DTL/Remote/IT-Quellen: 4Scotty, IT-Talents, Honeypot, EuropeRemotely, Remote.co, JustRemote, WhoIsHiring

INSERT INTO job_sources (id, code, name, enabled, config) VALUES
    (gen_random_uuid(), '4SCOTTY',     '4Scotty (DE IT)',         true,  '{}'::jsonb),
    (gen_random_uuid(), 'ITTALENTS',   'IT-Talents (DE)',         true,  '{}'::jsonb),
    (gen_random_uuid(), 'HONEYPOT',    'Honeypot (DE/EU Tech)',   true,  '{}'::jsonb),
    (gen_random_uuid(), 'EURREMOTE',   'EuropeRemotely',          true,  '{}'::jsonb),
    (gen_random_uuid(), 'REMOTECO',    'Remote.co',               true,  '{}'::jsonb),
    (gen_random_uuid(), 'JUSTRMOTE',   'JustRemote',              true,  '{}'::jsonb),
    (gen_random_uuid(), 'WHOWHIRING',  'Who Is Hiring (HN)',      true,  '{}'::jsonb)
ON CONFLICT (code) DO NOTHING;
