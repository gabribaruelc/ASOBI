# ASOBI — admin-auth-service (serviço de login dos admins)

Decide quem pode entrar no painel da loja. Ele guarda a lista de e-mails de admin e confere se o
login com Google é verdadeiro.

Só o backend da loja ([`backend`](backend.md)) conversa com ele. O navegador nunca chama este serviço direto.

- Java 17, Spring Boot 4.1, JPA (acesso ao banco) e Flyway (cria as tabelas)
- Banco: um esquema só dele no Supabase, chamado `admin_auth` (no computador usa o H2, um banco em arquivo)
- Roda na porta **8083** no computador
- No ar, fica no Google Cloud Run (serviço `asobi-admin-auth-service`), que desliga sozinho quando ninguém usa

## O que ele faz e o que não faz

| Faz | Não faz (isso é do backend) |
|---|---|
| Guardar a lista de e-mails de admin (tabela `admin_users`) | Mandar a pessoa para a tela do Google e manter ela logada (cookie) |
| Conferir o "ID token" que o Google devolve no login: se a assinatura é do Google, se não venceu, se foi feito para esta loja e se o e-mail foi verificado | Mostrar as telas do painel, inclusive a de "Admins" |
| Responder se um e-mail ainda está na lista | Saber qualquer coisa de produtos, pedidos ou clientes |
| Cuidar das regras: não repetir e-mail e ninguém remover a si mesmo | |
| Criar o primeiro admin (`ADMIN_BOOTSTRAP_EMAIL`) quando a lista está vazia | |

### Como o login funciona, passo a passo

1. O backend manda a pessoa para a tela do Google.
2. O Google devolve um "ID token" (uma prova de quem a pessoa é).
3. O backend envia esse token para `POST /api/logins` e este serviço diz se a pessoa é admin.
4. Depois, a cada clique no painel, o backend pergunta em `GET /api/authorizations` se o e-mail ainda está
   na lista. A resposta fica guardada por 30 segundos para não perguntar toda hora.

Por causa do passo 4, quando alguém é removido da lista, perde o acesso em até 30 segundos, sem
precisar esperar o login expirar.

## Rotas

Toda rota que começa com `/api/` exige o cabeçalho `X-Api-Key` com a mesma senha de `ADMIN_AUTH_SERVICE_API_KEY`.

| Rota | O que faz |
|---|---|
| `POST /api/logins` | Recebe `{"idToken": "<token do Google>"}` e responde `{"email": "...", "admin": true}` |
| `GET /api/authorizations?email=` | Responde `{"email": "...", "admin": true\|false}` |
| `GET /api/admins` | Lista os admins em ordem alfabética: `[{id, email, createdByEmail, createdAt}]` |
| `POST /api/admins` | Adiciona um admin. Recebe `{"email": "...", "createdByEmail": "..."}` e responde `201` |
| `DELETE /api/admins/{id}?requestedBy=` | Tira o acesso. `requestedBy` é o e-mail de quem está removendo |
| `GET /actuator/health` | Diz se o serviço está no ar. Não precisa de senha |

Quando dá erro, a resposta vem em JSON com a explicação:
`400` dados errados · `401` senha errada ou token do Google inválido · `403` e-mail não verificado ou fora da
lista · `404` admin não encontrado · `409` e-mail já cadastrado ou tentou remover a si mesmo ·
`503` falta o `GOOGLE_CLIENT_ID`.

## Como rodar no computador

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Ele sobe com o banco H2 na pasta `data/admin-auth` e com `admin@asobi.com.br` como primeiro admin.
Como o banco fica em arquivo, os admins adicionados continuam lá depois de reiniciar.
No computador, o backend já procura este serviço em `http://localhost:8083`.
Outra opção é subir tudo junto com `docker compose up --build` na pasta principal do projeto.

Testes: `.\mvnw.cmd test`

## Configurações (variáveis de ambiente)

| Variável | O que colocar |
|---|---|
| `ADMIN_AUTH_SERVICE_API_KEY` | uma senha longa e aleatória, igual à do backend. Se ficar vazia, não confere a senha (só use assim no computador) |
| `ADMIN_BOOTSTRAP_EMAIL` | conta Google do primeiro admin (só é usada se a lista estiver vazia) |
| `GOOGLE_CLIENT_ID` | o **mesmo** ID de cliente do Google usado no backend. Vazio = login com Google desligado |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (com o perfil `supabase`) |
| `DB_USER` / `DB_PASSWORD` | usuário e senha deste serviço no Supabase (veja abaixo) |
| `DB_SCHEMA` | opcional; se não preencher, usa `admin_auth` |

### Criando o usuário e o esquema no Supabase

O serviço usa o mesmo projeto do Supabase que a loja (para continuar no plano grátis), mas com um
esquema e um usuário separados, sem acesso às tabelas do backend. No SQL Editor do Supabase, uma vez só:

```sql
CREATE SCHEMA admin_auth;
CREATE ROLE admin_auth_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA admin_auth TO admin_auth_service;
ALTER ROLE admin_auth_service SET search_path = admin_auth;
```

Pelo Session pooler do Supabase, o `DB_USER` fica `admin_auth_service.<id-do-projeto>`.
As tabelas são criadas sozinhas pelo Flyway na primeira vez que o serviço sobe.

## Colocar no ar (Cloud Run)

Dentro da pasta `admin-auth-service/`:

```bash
gcloud run deploy asobi-admin-auth-service \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 1 \
  --set-env-vars ADMIN_BOOTSTRAP_EMAIL=<email-google-do-admin> \
  --set-secrets ADMIN_AUTH_SERVICE_API_KEY=asobi-admin-auth-api-key:latest,GOOGLE_CLIENT_ID=asobi-google-client-id:latest,DB_URL=asobi-db-url:latest,DB_USER=asobi-admin-auth-db-user:latest,DB_PASSWORD=asobi-admin-auth-db-password:latest
```

Depois, no backend, coloque `ADMIN_AUTH_SERVICE_URL=<endereço-deste-serviço>` e a mesma senha
(`asobi-admin-auth-api-key`) em `ADMIN_AUTH_SERVICE_API_KEY`.
O `--allow-unauthenticated` só deixa o serviço acessível pela internet; quem barra estranhos é a senha do `X-Api-Key`.

Coloque este serviço no ar **antes** do backend. Sem ele, ninguém entra no painel (na dúvida, o backend
sempre bloqueia). A loja continua vendendo normalmente.
