# ASOBI — two-factor-service (microsserviço de autenticação de dois fatores)

Confirma que uma pessoa tem acesso a um e-mail: gera um código de 6 números, envia por e-mail e depois
confere o código que ela digita. É chamado **só pelo backend da loja** (`../backend`), nunca direto pelo navegador.

- Java 17 · Spring Boot 4.1 · JPA + Flyway
- Banco próprio: esquema `two_factor` no Supabase (H2 em arquivo localmente)
- Porta: **8082** localmente (`PORT` no Cloud Run)
- Hospedagem: Google Cloud Run, serviço próprio (`asobi-two-factor-service`), escala a zero

## Responsabilidade (e o que NÃO é dele)

| Faz | Não faz (fica no backend) |
|---|---|
| Gerar o código (6 números, sorteio criptográfico) e enviar pelo Resend | Decidir quando pedir o segundo fator e para quem |
| Guardar só o HMAC do código, nunca o código | Mostrar a tela em que a pessoa digita o código |
| Conferir o código: validade de 10 minutos, uso único, no máximo 5 erros | Manter a sessão de quem passou na conferência |
| Limitar os pedidos: 5 códigos por e-mail a cada 15 minutos | Saber qualquer coisa de produtos, pedidos ou admins |
| Aposentar o código anterior quando a pessoa pede outro | |

O fluxo: o backend chama `POST /api/challenges` com o e-mail e a finalidade (`purpose`, ex.: `admin-login`) e
guarda o `id` devolvido. A pessoa recebe o código por e-mail e digita na tela do backend, que chama
`POST /api/challenges/{id}/verifications`. Só passa quem receber `verified: true`; o backend ainda confere se
o `email` e o `purpose` da resposta são os que ele esperava.

O código nunca volta em nenhuma resposta da API: só existe no e-mail. No banco fica o HMAC-SHA256 dele, feito
com uma chave que só o serviço tem (`TWO_FACTOR_CODE_SECRET`). Um hash comum não bastaria, porque há só um
milhão de códigos possíveis.

## API

Todas as rotas `/api/**` exigem o cabeçalho `X-Api-Key` (igual a `TWO_FACTOR_SERVICE_API_KEY`).

| Método e rota | Descrição |
|---|---|
| `POST /api/challenges` | Corpo: `{"email": "...", "purpose": "admin-login"}`. Envia o código e responde `201` com `{id, email, purpose, expiresAt}` |
| `POST /api/challenges/{id}/verifications` | Corpo: `{"code": "123456"}`. Código certo: `200` com `{id, email, purpose, verified: true}` |
| `GET /actuator/health` | Health check (aberto, sem chave) |

`purpose` aceita letras minúsculas, números e hífen (até 40 caracteres).

Erros (`application/problem+json`, com `detail` pronto para mostrar na tela):

| Status | Quando |
|---|---|
| `400` | Dados inválidos (`errors` traz a mensagem de cada campo) |
| `401` | Chave de API errada |
| `404` | Desafio não encontrado |
| `410` | Código vencido, já usado ou bloqueado por excesso de erros: é preciso pedir outro |
| `422` | Código incorreto; `attemptsLeft` diz quantas tentativas restam |
| `429` | E-mail pediu códigos demais em pouco tempo |
| `503` | O e-mail não pôde ser enviado; nenhum código fica valendo |

## Rodar localmente

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Sobe com H2 em `data/two-factor`. Sem `RESEND_API_KEY` nada é enviado: o e-mail, **com o código**, aparece no
log do serviço, que é como se testa o fluxo localmente. Ou suba tudo junto com `docker compose up --build`
na raiz do repositório.

```bash
curl -X POST localhost:8082/api/challenges -H "Content-Type: application/json" \
  -d '{"email": "admin@asobi.com.br", "purpose": "admin-login"}'
# pegue o código no log e o id na resposta
curl -X POST localhost:8082/api/challenges/<id>/verifications -H "Content-Type: application/json" \
  -d '{"code": "123456"}'
```

Testes: `.\mvnw.cmd test`

## Variáveis de ambiente

| Variável | Exemplo |
|---|---|
| `TWO_FACTOR_SERVICE_API_KEY` | chave longa aleatória, a mesma configurada no backend. Vazia = sem conferência (só local) |
| `TWO_FACTOR_CODE_SECRET` | chave longa aleatória do HMAC dos códigos. **Obrigatória em produção**: vazia, o serviço sorteia uma a cada subida e, como o Cloud Run escala a zero, os códigos pendentes deixariam de valer |
| `RESEND_API_KEY` | chave do Resend. Vazia = o e-mail só aparece no log, com o código (nunca em produção) |
| `EMAIL_FROM` | `ASOBI <acesso@asobi.com.br>`; o domínio precisa estar verificado no Resend |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (perfil `supabase`) |
| `DB_USER` / `DB_PASSWORD` | usuário próprio deste serviço no Supabase (abaixo) |
| `DB_SCHEMA` | opcional; padrão `two_factor` |

Validade do código, número de tentativas e limite de pedidos ficam em `two-factor.*` no `application.yml`.

### Usuário e esquema no Supabase

O serviço usa o mesmo projeto Supabase da loja (para ficar no free tier), mas com esquema e usuário
separados, sem acesso às tabelas do backend. No SQL Editor do Supabase, uma única vez:

```sql
CREATE SCHEMA two_factor;
CREATE ROLE two_factor_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA two_factor TO two_factor_service;
ALTER ROLE two_factor_service SET search_path = two_factor;
```

Pelo Session pooler, o `DB_USER` fica `two_factor_service.<id-do-projeto>`. A tabela `challenges` é criada
pelo Flyway na primeira subida. Linhas vencidas há mais de um dia são apagadas pelo próprio serviço a cada
novo pedido de código.

## Deploy no Cloud Run

De dentro de `two-factor-service/`:

```bash
gcloud run deploy asobi-two-factor-service \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 1 \
  --set-env-vars "EMAIL_FROM=ASOBI <acesso@asobi.com.br>" \
  --set-secrets TWO_FACTOR_SERVICE_API_KEY=asobi-two-factor-api-key:latest,TWO_FACTOR_CODE_SECRET=asobi-two-factor-code-secret:latest,RESEND_API_KEY=asobi-resend-api-key:latest,DB_URL=asobi-db-url:latest,DB_USER=asobi-two-factor-db-user:latest,DB_PASSWORD=asobi-two-factor-db-password:latest
```

(`--allow-unauthenticated` libera o acesso de rede; quem protege a API é a `X-Api-Key`.)

Cada código é um e-mail do plano gratuito do Resend (100 por dia, somados aos e-mails de pedido).
