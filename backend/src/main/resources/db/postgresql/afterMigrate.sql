-- Callback do Flyway (só Postgres).
-- O Supabase expõe o schema public no PostgREST com a chave anon, que é pública.
-- A loja não usa essa API, então o RLS fica ligado sem nenhuma policy: bloqueia
-- o PostgREST e o backend (dono das tabelas) continua com acesso normal.
DO $$
DECLARE
    t record;
BEGIN
    FOR t IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public' AND NOT rowsecurity
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;
END
$$;
