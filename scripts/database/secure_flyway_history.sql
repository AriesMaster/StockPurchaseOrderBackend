-- Keep migration metadata accessible to the migration owner, not API clients.
ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;
REVOKE ALL PRIVILEGES ON TABLE public.flyway_schema_history FROM anon, authenticated, PUBLIC;
