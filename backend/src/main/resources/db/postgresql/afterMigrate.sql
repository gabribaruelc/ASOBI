-- Callback do Flyway: roda depois de toda migração, só no Postgres (Supabase).
--
-- O Supabase publica as tabelas do schema public na API REST dele (PostgREST),
-- acessível com a chave "anon", que é pública. A loja não usa essa API — tudo
-- passa pelo backend —, então ligamos o RLS sem nenhuma política: a API do
-- Supabase fica sem acesso e o backend, que conecta como dono das tabelas,
-- continua lendo e gravando normalmente.
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
