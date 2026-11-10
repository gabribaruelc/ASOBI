# ASOBI — status-service (aviso de site fora do ar)

Confere, a cada 5 minutos, se a loja e os serviços dela estão respondendo e se o que eles usam por fora
(banco, e-mails, pagamentos, fotos, frete) está funcionando. Quando algo para, manda um e-mail para
**todos os admins cadastrados no painel** dizendo o que parou e o que fazer; quando volta, manda outro
avisando.

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

### O que cada serviço usa por fora

O `/actuator/health` de cada serviço Java lista as partes de que ele depende, cada uma com `UP` ou
`DOWN` (`show-components: always`, sem detalhes). O status-service lê essa lista e avisa a parte que
parou pelo nome, em vez de dizer só que "a API deu erro":

| Parte | Quem confere | Como | Nome no e-mail |
|---|---|---|---|
| `db` | backend, two-factor, admin-auth, notification | conexão com o banco (do próprio Spring) | Banco de dados (Supabase) |
| `resend` | backend (com `RESEND_API_KEY`) | `GET /domains`: chave aceita e domínio do remetente verificado | E-mails da loja (Resend) |
| `mercadoPago` | backend (com `MERCADO_PAGO_ACCESS_TOKEN`) | `GET /users/me`: token aceito | Pagamentos (Mercado Pago) |
| `supabaseStorage` | backend (com `SUPABASE_URL`/`SUPABASE_SERVICE_KEY`) | consulta o bucket das fotos | Fotos dos produtos (Supabase Storage) |
| `melhorEnvio` | shipping-service (com `MELHOR_ENVIO_TOKEN`) | `GET /api/v2/me`: token aceito (ele vence) | Frete automático (Melhor Envio) |

A mesma parte em vários serviços (o banco, por exemplo) vira um item só no e-mail. O resultado do
health do backend e do shipping-service fica guardado por 60 segundos, para que acessos repetidos não
virem chamadas ao Resend, ao Mercado Pago e ao Melhor Envio. O nome e o "o que fazer" de cada item ficam
em `status.targets[].hint` e `status.dependencies` no `application.yml`; parte sem nome lá aparece com o
nome técnico.

## Quando o e-mail sai

- **Parou**: depois de **2 falhas seguidas** (cerca de 10 minutos). Uma falha só não avisa, porque o
  Cloud Run e o Supabase às vezes demoram para "acordar".
- **Continua parado**: se não voltar, outro aviso a cada 6 horas.
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
- Se o Resend estiver fora do ar, o aviso só sai quando ele voltar. Isso vale também para o aviso de
  "E-mails da loja (Resend)" parou: como o status-service usa a mesma chave do Resend, ele só chega se o
  problema for algo que não impede o envio dele (por exemplo, o domínio da loja perdeu a verificação e o
  `EMAIL_FROM` daqui usa outro), ou quando o Resend voltar. Os registros do Cloud Run mostram a falha.
- Se um serviço não responde, as partes dele não são conferidas nessa rodada (ficam como estavam).

## Rotas

| Rota | O que faz |
|---|---|
| `POST /api/checks` | Faz uma rodada de conferência e devolve `[{name, label, up, detail}]` (serviços e, com o prefixo `dependency:`, as partes). Exige `X-Api-Key` |
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
