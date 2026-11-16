-- Domínio definitivo da loja: asobistore.app. Só troca o e-mail se o lojista ainda não editou.
UPDATE site_info
SET contact_email = 'contato@asobistore.app'
WHERE id = 1 AND contact_email = 'contato@asobi.com.br';
