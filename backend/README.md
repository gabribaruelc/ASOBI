# ASOBI — backend (Java / Spring Boot)

Backend da loja ASOBI: API REST (JSON) consumida pela loja em Next.js (`../frontend`)
e painel admin renderizado no servidor com Thymeleaf.

- Java 17 · Spring Boot 4.1 · Spring Data JPA · Flyway · Thymeleaf
- Banco: Supabase (PostgreSQL) em produção; H2 em arquivo (modo PostgreSQL) no desenvolvimento local
- Hospedagem: Google Cloud Run (imagem gerada pelo `Dockerfile`)

## Rodar localmente

Não precisa instalar Maven nem banco — o wrapper baixa o Maven e o perfil `local` usa H2.

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

- http://localhost:8080/ — página de status (Thymeleaf)
- http://localhost:8080/api/health — `{"status":"ok"}`
- http://localhost:8080/h2-console — console do banco local (JDBC URL: `jdbc:h2:file:./data/asobi`, usuário `sa`, sem senha)

Testes: `.\mvnw.cmd test`

> No Git Bash do Windows, use `mvnw.cmd` (o `mvnw` em shell falha ao instalar o Maven por permissão em `/tmp`).

## Variáveis de ambiente (perfil `prod`)

| Variável | Exemplo |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (já definido no Dockerfile) |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (Supabase → Connect → **Session pooler**) |
| `DB_USER` | `postgres.<id-do-projeto>` |
| `DB_PASSWORD` | senha do banco no Supabase |
| `FRONTEND_ORIGINS` | `https://asobi.com.br,https://www.asobi.com.br` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | credenciais OAuth do Google (login do painel) |
| `ADMIN_BOOTSTRAP_EMAIL` | conta Google do primeiro admin (usado só se ainda não houver nenhum) |

Nunca commitar essas credenciais.

## Painel admin

`/admin` — Thymeleaf, renderizado pelo backend. Só entra quem faz login com Google **e** tem o
e-mail cadastrado em `/admin/admins` (conferido no banco a cada requisição). A sessão fica no
banco (Spring Session JDBC), então sobrevive ao Cloud Run desligar a instância.

- **Local**: tela de login tem um "modo desenvolvimento" que entra só com o e-mail
  (admin inicial `dona@asobi.com.br`). Nunca ligado em produção (`asobi.admin.dev-login-enabled`).
- **Google (produção)**: no [Google Cloud Console](https://console.cloud.google.com/apis/credentials),
  crie um "ID do cliente OAuth" do tipo *Aplicativo da Web* com a URI de redirecionamento
  `https://<url-do-cloud-run>/login/oauth2/code/google` (e `http://localhost:8080/login/oauth2/code/google`
  para testar localmente com `SPRING_PROFILES_ACTIVE=local,google`).

## Deploy no Cloud Run

Com o [gcloud CLI](https://cloud.google.com/sdk/docs/install) instalado e logado, de dentro de `backend/`:

```bash
gcloud run deploy asobi-backend \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 2 \
  --set-env-vars FRONTEND_ORIGINS=https://asobi.com.br \
  --set-env-vars ADMIN_BOOTSTRAP_EMAIL=<email-google-da-donna> \n  --set-secrets DB_URL=asobi-db-url:latest,DB_USER=asobi-db-user:latest,DB_PASSWORD=asobi-db-password:latest,GOOGLE_CLIENT_ID=asobi-google-client-id:latest,GOOGLE_CLIENT_SECRET=asobi-google-client-secret:latest
```

`--min-instances 0` mantém o custo perto de zero (o primeiro acesso após um período parado leva alguns segundos para acordar).

## Estrutura

```
src/main/java/br/com/asobi/
  config/     configurações transversais (CORS, segurança, Mercado Pago)
  common/     status/health, tratamento de erros
  <domínio>/  catalog, review, order, payment, content, admin, shipping
              cada um com controller / service / repository / model / dto
src/main/resources/
  db/migration/  migrações Flyway (V1, V2, ...)
  templates/     páginas Thymeleaf (painel admin)
```
