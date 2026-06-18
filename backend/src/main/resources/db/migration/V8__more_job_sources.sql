-- Weitere Job-Quellen: WeWorkRemotely, Workable, Jobspresso, NoDesk, CareerJet, ZipRecruiter

INSERT INTO job_sources (id, code, name, enabled, config) VALUES
    (gen_random_uuid(), 'WWREMOTE',    'We Work Remotely',     true,  '{}'::jsonb),
    (gen_random_uuid(), 'WORKABLE',    'Workable (ATS)',       false, '{}'::jsonb),
    (gen_random_uuid(), 'JOBSPRESSO',  'Jobspresso',          true,  '{}'::jsonb),
    (gen_random_uuid(), 'NODESK',      'NoDesk',              true,  '{}'::jsonb),
    (gen_random_uuid(), 'CAREERJET',   'CareerJet',           false, '{}'::jsonb),
    (gen_random_uuid(), 'ZIPRECRUITER','ZipRecruiter',        false, '{}'::jsonb)
ON CONFLICT (code) DO NOTHING;
