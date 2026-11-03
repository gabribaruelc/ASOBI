# ASOBI — status-service (aviso de site fora do ar)

Confere, a cada 5 minutos, se a loja e os serviços dela estão respondendo. Quando algo cai, manda um
e-mail para **todos os admins cadastrados no painel**; quando volta, manda outro avisando.

Ninguém chama este serviço a não ser a rotina do Google Cloud Scheduler. Ele não muda nada na loja.

- Java 17, Spring Boot 4.1, JPA e Flyway
- Banco: um esquema só dele no Supabase, chamado `status` (no computador usa o H2, um banco em arquivo)
- Roda na porta **8085** no computador
- No ar, fica no Google Cloud Run (serviço `asobi-status-service`)

## O que ele confere

| Nome | O que aparece no e-mail | Endereço conferido |
|---|---|---|
| `store` | Loja (site) | `STORE_URL` (a página inicial) |
| `backend` | API da loja e painel admin | `BACKEND_URL` + `/actuator/health` (inclui o banco do Supabase) |
| `shipping-service` | Cálculo de frete | `SHIPPING_SERVICE_URL` + `/actuator/health` |
| `two-factor-service` | Código de acesso do painel | `TWO_FACTOR_SERVICE_URL` + `/actuator/health` |
| `admin-auth-service` | Lista de admins e login do painel | `ADMIN_AUTH_SERVICE_URL` + `/actuator/health` |
| `notification-service` | Avisos de produto (avise-me) | `NOTIFICATION_SERVICE_URL` + `/actuator/health` |

Endereço vazio = fica de fora (útil enquanto um serviço ainda não foi para o ar). Conta como "no ar"
qualquer resposta de sucesso (HTTP 2xx) em até 30 segundos.

## Quando o e-mail sai

- **Fora do ar**: depois de **2 falhas seguidas** (cerca de 10 minutos). Uma falha só não avisa, porque o
  Cloud Run e o Supabase às vezes demoram para "acordar".
- **Continua fora do ar**: se não voltar, outro aviso a cada 6 horas.
- **Voltou**: um e-mail quando volta a responder, dizendo quanto tempo ficou fora. Só sai se o aviso de
  "fora do ar" saiu antes.
- Vários serviços caindo juntos viram **um e-mail só**.
- Se o envio falhar (Resend fora do ar), nada é marcado como avisado e a próxima rodada tenta de novo.

### Para quem vai

Para a lista de admins do painel, lida do `admin-auth-service` (`GET /api/admins`) a cada rodada. Quem é
adicionado ou removido no painel passa a receber (ou deixa de receber) sem mexer em nada aqui.

O próprio `admin-auth-service` pode estar entre os que caíram. Por isso a lista lida fica guardada no
banco deste serviço (`known_admins`) e é usada quando ele não responde. Se a lista nunca pôde ser lida,
o aviso vai para `STATUS_FALLBACK_EMAILS` (padrão `admin@asobi.com.br`).

### O que ele não consegue avisar

- Se o próprio status-service ou o Cloud Scheduler parar, não sai aviso nenhum. Os dois são do Google e
  raramente caem junto com o resto, mas não há um "vigia do vigia".
- Se o Resend estiver fora do ar, o aviso só sai quando ele voltar.

## Rotas

| Rota | O que faz |
|---|---|
| `POST /api/checks` | Faz uma rodada de conferência e devolve `[{name, label, up, detail}]`. Exige `X-Api-Key` |
| `GET /actuator/health` | Diz se o serviço está no ar. Não precisa de senha |

## Como rodar no computador

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Ele sobe com o banco H2 na pasta `data/status` e confere os serviços em `localhost` (portas 3000 e
8080 a 8084). Nada roda sozinho no computador: para fazer uma rodada, chame a rota (duas vezes seguidas
para ver o aviso de "fora do ar"):

```bash
curl -X POST localhost:8085/api/checks
```

Sem `RESEND_API_KEY`, o e-mail só aparece no log. Também sobe junto com `docker compose up --build`.

Testes: `.\mvnw.cmd test`

## Configurações (variáveis de ambiente)

| Variável | O que colocar |
|---|---|
| `STATUS_SERVICE_API_KEY` | uma senha longa e aleatória; a rotina do Cloud Scheduler manda no `X-Api-Key`. Vazia = não confere (só no computador) |
| `ADMIN_AUTH_SERVICE_URL` / `ADMIN_AUTH_SERVICE_API_KEY` | endereço e chave do `admin-auth-service` (a mesma chave do backend) |
| `STATUS_FALLBACK_EMAILS` | para quem avisar se a lista de admins nunca pôde ser lida; separados por vírgula. Padrão `admin@asobi.com.br` |
| `STORE_URL`, `BACKEND_URL`, `SHIPPING_SERVICE_URL`, `TWO_FACTOR_SERVICE_URL`, `NOTIFICATION_SERVICE_URL` | endereços conferidos (veja a tabela lá em cima) |
| `RESEND_API_KEY` | chave do Resend. Vazia = o aviso só aparece no log |
| `EMAIL_FROM` | `ASOBI <avisos@asobi.com.br>`; o domínio precisa estar verificado no Resend |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | banco no Supabase (com o perfil `supabase`; veja abaixo) |
| `DB_SCHEMA` | opcional; se não preencher, usa `status` |

O número de falhas antes de avisar, o tempo de espera e o intervalo do lembrete ficam em `status.*` no
`application.yml`.

### Criando o usuário e o esquema no Supabase

No SQL Editor do Supabase, uma vez só:

```sql
CREATE SCHEMA status;
CREATE ROLE status_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA status TO status_service;
ALTER ROLE status_service SET search_path = status;
```

Pelo Session pooler do Supabase, o `DB_USER` fica `status_service.<id-do-projeto>`. Coloque usuário e
senha no `status-service/.env` antes de rodar `bash scripts/create-secrets.sh`. As tabelas são criadas
pelo Flyway na primeira vez que o serviço sobe.

## Colocar no ar (Cloud Run)

Depois do deploy de todos os outros (precisa das URLs deles), dentro da pasta `status-service/`:

```bash
gcloud run deploy asobi-status-service \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 1 \
  --set-env-vars "EMAIL_FROM=ASOBI <avisos@asobi.com.br>,STORE_URL=https://asobi.com.br,BACKEND_URL=<url-do-backend>,SHIPPING_SERVICE_URL=<url-do-shipping-service>,TWO_FACTOR_SERVICE_URL=<url-do-two-factor-service>,ADMIN_AUTH_SERVICE_URL=<url-do-admin-auth-service>,NOTIFICATION_SERVICE_URL=<url-do-notification-service>" \
  --set-secrets STATUS_SERVICE_API_KEY=asobi-status-api-key:latest,ADMIN_AUTH_SERVICE_API_KEY=asobi-admin-auth-api-key:latest,RESEND_API_KEY=asobi-resend-api-key:latest,DB_URL=asobi-db-url:latest,DB_USER=asobi-status-db-user:latest,DB_PASSWORD=asobi-status-db-password:latest
```

Depois crie a rotina de 5 em 5 minutos:

```bash
bash scripts/create-status-check-job.sh <url-do-status-service>
```

### Custo

Fica no plano grátis:
- **Cloud Scheduler**: 3 rotinas grátis por conta; a loja usa 2 (esta e o lembrete de pagamento).
- **Cloud Run**: cerca de 8.600 chamadas por mês para cada serviço, bem dentro das 2 milhões grátis. Só se
  paga o tempo em que a requisição está sendo atendida, e uma conferência leva menos de um segundo.
  Como efeito colateral, os serviços ficam "acordados" e a loja abre mais rápido para o cliente.
- **Supabase**: a conferência do backend passa pelo banco, então o projeto do plano grátis não pausa
  mais por falta de uso.
- **Resend**: só gasta e-mail quando algo cai ou volta.
