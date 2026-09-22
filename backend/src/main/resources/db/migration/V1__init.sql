-- Migração inicial vazia: só registra a linha de base do Flyway.
-- As tabelas de cada domínio (catálogo, avaliações, pedidos...) entram nas
-- próximas migrações (V2, V3, ...), uma por etapa da migração.
SELECT 1;
