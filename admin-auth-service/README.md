# ASOBI — admin-auth-service (microsserviço de autenticação de admins)

Diz quem pode entrar no painel da loja: guarda a lista de e-mails de admin e confere o login com Google.
É chamado **só pelo backend da loja** (`../backend`), nunca direto pelo navegador.

- Java 17 · Spring Boot 4.1 · JPA + Flyway
- Banco próprio: esquema `admin_auth` no Supabase (H2 em arquivo localmente)
- Porta: **8083** localmente (`PORT` no Cloud Run)
- Hospedagem: Google Cloud Run, serviço próprio (`asobi-admin-auth-service`), escala a zero

## Responsabilidade (e o que NÃO é dele)

| Faz | Não faz (fica no backend) |
|---|---|
| Guardar a lista de e-mails de admin (tabela `admin_users`) | Redirecionar o navegador para o Google e manter a sessão (cookie) do painel |
| Conferir o ID token do Google: assinatura, validade, emissor, destinatário e e-mail verificado | Renderizar as telas do painel, inclusive a de "Admins" |
| Responder se um e-mail continua na lista | Saber qualquer coisa de produtos, pedidos ou clientes |
| Regras da lista: e-mail sem repetição, ninguém remove a si mesmo | |
| Criar o primeiro admin (`ADMIN_BOOTSTRAP_EMAIL`) quando a lista está vazia | |

O login funciona assim: o backend manda a pessoa para o Google, recebe o ID token e o repassa para
`POST /api/logins`. Só entra quem este serviço liberar. Depois disso, a cada requisição do painel o backend
pergunta em `GET /api/authorizations` se o e-mail continua na lista (e guarda a resposta por 30 segundos),
então remover um admin corta o acesso sem esperar a sessão expirar.

## API

Todas as rotas `/api/**` exigem o cabeçalho `X-Api-Key` (igual a `ADMIN_AUTH_SERVICE_API_KEY`).

| Método e rota | Descrição |
|---|---|
| `POST /api/logins` | Corpo: `{"idToken": "<ID token do Google>"}`. Resposta: `{"email": "...", "admin": true}` |
| `GET /api/authorizations?email=` | `{"email": "...", "admin": true\|false}` |
| `GET /api/admins` | `[{id, email, createdByEmail, createdAt}]`, em ordem alfabética |
| `POST /api/admins` | Corpo: `{"email": "...", "createdByEmail": "..."}`. Resposta `201` com o admin criado |
| `DELETE /api/admins/{id}?requestedBy=` | Remove o acesso. `requestedBy` é o e-mail de quem está removendo |
| `GET /actuator/health` | Health check (aberto, sem chave) |

Erros (`application/problem+json`): `400` dados inválidos · `401` chave errada ou token do Google inválido ·
`403` e-mail não verificado ou fora da lista · `404` admin não encontrado · `409` e-mail já cadastrado ou
tentativa de remover a si mesmo · `503` login Google sem `GOOGLE_CLIENT_ID`.

## Rodar localmente

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Sobe com H2 em `data/admin-auth` e `admin@asobi.com.br` como admin inicial. O arquivo fica em disco:
os admins adicionados pelo painel continuam lá depois de reiniciar. Localmente o backend já procura este
serviço em `http://localhost:8083`. Ou suba tudo junto com `docker compose up --build` na raiz do repositório.

Testes: `.\mvnw.cmd test`

## Variáveis de ambiente

| Variável | Exemplo |
|---|---|
| `ADMIN_AUTH_SERVICE_API_KEY` | chave longa aleatória, a mesma configurada no backend. Vazia = sem conferência (só local) |
| `ADMIN_BOOTSTRAP_EMAIL` | conta Google do primeiro admin (usada só se a lista estiver vazia) |
| `GOOGLE_CLIENT_ID` | o **mesmo** ID de cliente OAuth do backend; o token precisa ter sido emitido para ele. Vazio = login Google indisponível |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (perfil `supabase`) |
| `DB_USER` / `DB_PASSWORD` | usuário próprio deste serviço no Supabase (abaixo) |
| `DB_SCHEMA` | opcional; padrão `admin_auth` |

### Usuário e esquema no Supabase

O serviço usa o mesmo projeto Supabase da loja (para ficar no free tier), mas com esquema e usuário
separados, sem acesso às tabelas do backend. No SQL Editor do Supabase, uma única vez:

```sql
CREATE SCHEMA admin_auth;
CREATE ROLE admin_auth_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA admin_auth TO admin_auth_service;
ALTER ROLE admin_auth_service SET search_path = admin_auth;
```

Pelo Session pooler, o `DB_USER` fica `admin_auth_service.<id-do-projeto>`. As tabelas são criadas pelo
Flyway na primeira subida.

## Deploy no Cloud Run

De dentro de `admin-auth-service/`:

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

Depois, no backend: `ADMIN_AUTH_SERVICE_URL=<url-deste-serviço>` e o mesmo segredo `asobi-admin-auth-api-key`
em `ADMIN_AUTH_SERVICE_API_KEY`. (`--allow-unauthenticated` libera o acesso de rede; quem protege a API é a `X-Api-Key`.)

Suba este serviço **antes** do backend: sem ele no ar ninguém entra no painel (o backend nunca libera na
dúvida). A loja continua vendendo normalmente.
