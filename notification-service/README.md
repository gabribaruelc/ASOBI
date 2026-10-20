# ASOBI — notification-service (microsserviço de avisos de produto)

Avisa por e-mail quem pediu para saber quando um jogo **entrou em promoção** ou **voltou ao estoque**.
Guarda os pedidos de aviso, recebe do backend os eventos de preço e estoque e dispara os e-mails, cada um com
o seu link de descadastro. A API é chamada **só pelo backend da loja** (`../backend`); o navegador só acessa a
página de descadastro.

- Java 17 · Spring Boot 4.1 · JPA + Flyway · Thymeleaf (e-mail e página de descadastro)
- Banco próprio: esquema `notification` no Supabase (H2 em arquivo localmente)
- Porta: **8084** localmente (`PORT` no Cloud Run)
- Hospedagem: Google Cloud Run, serviço próprio (`asobi-notification-service`), escala a zero

## Responsabilidade (e o que NÃO é dele)

| Faz | Não faz (fica no backend) |
|---|---|
| Guardar os pedidos de aviso (e-mail + produto + tipo), só com consentimento | Mostrar o formulário "avise-me" na página do jogo |
| Enviar os e-mails pelo Resend quando chega um evento | Perceber que o preço ou o estoque mudou e montar o evento |
| Não repetir o mesmo aviso para a mesma pessoa em menos de 24 horas | Saber o que é um produto: nome, preço e link vêm no evento |
| Descadastro pelo link do e-mail, sem login | Enviar os e-mails de pedido e a newsletter |
| Apagar tudo de um e-mail (LGPD) ou de um produto que saiu do catálogo | |

### Dados pessoais (LGPD)

- O serviço guarda **só o e-mail do adulto responsável** e o id do produto. Nenhum dado de criança.
- A inscrição só é aceita com `consent: true`, e a data fica gravada em `consented_at`. O formulário da loja
  precisa ter uma caixa de consentimento **desmarcada por padrão**.
- Todo e-mail traz o link de descadastro, que apaga a linha na hora (não fica "inativa").
- Pedido de exclusão: `DELETE /api/subscriptions?email=`. Pedido de acesso: `GET /api/subscriptions?email=`.

## API

Todas as rotas `/api/**` exigem o cabeçalho `X-Api-Key` (igual a `NOTIFICATION_SERVICE_API_KEY`).

| Método e rota | Descrição |
|---|---|
| `POST /api/subscriptions` | Corpo: `{"email": "...", "productId": 7, "type": "PROMOTION", "consent": true}`. `201` na inscrição nova, `200` se a pessoa já tinha pedido esse aviso. Resposta: `{id, email, productId, type, consentedAt, lastNotifiedAt}` |
| `GET /api/subscriptions?email=&productId=` | Lista, da mais nova para a mais antiga. Os dois filtros são opcionais |
| `DELETE /api/subscriptions/{id}` | Remove uma inscrição (`204`) |
| `DELETE /api/subscriptions?email=` | Remove tudo de um e-mail. Resposta: `{"removed": 2}` |
| `DELETE /api/subscriptions?productId=` | Remove tudo de um produto (ele saiu do catálogo) |
| `POST /api/product-events` | O backend conta que um produto mudou (abaixo). Resposta: `{subscribers, sent, failed, skipped}` |
| `GET /actuator/health` | Health check (aberto, sem chave) |

`type` é `PROMOTION`, `BACK_IN_STOCK` ou `LOW_STOCK` ("está acabando"), tanto na inscrição quanto no evento.

Evento:

```json
{
  "type": "PROMOTION",
  "productId": 7,
  "productName": "Corrida dos Sapos",
  "productUrl": "https://asobi.com.br/jogos/corrida-dos-sapos",
  "price": 79.90,
  "originalPrice": 99.90
}
```

`price` é o que a pessoa paga agora; `originalPrice` é opcional e aparece riscado quando é maior que `price`.
`stock` é opcional: no `LOW_STOCK`, vira o "Restam só N unidades" do e-mail.
Na resposta, `skipped` conta quem já tinha recebido o mesmo aviso nas últimas 24 horas, e `failed`, quem não
recebeu porque o Resend recusou o lote. Quem falhou não é marcado como avisado: basta o backend mandar o
evento de novo.

Erros (`application/problem+json`): `400` dados inválidos ou sem consentimento · `401` chave errada ·
`404` inscrição não encontrada.

### Página de descadastro (aberta, sem chave)

| Método e rota | Descrição |
|---|---|
| `GET /descadastro/{token}` | Página de confirmação. Não cancela nada: antivírus e programas de e-mail abrem os links sozinhos |
| `POST /descadastro/{token}` | Cancela aquele aviso. Com `all=true`, cancela todos os avisos do mesmo e-mail |

Quem autoriza é o token do link (256 bits, um por inscrição). Token desconhecido mostra a mesma página de
"aviso cancelado", para o link poder ser aberto mais de uma vez. Os e-mails também levam os cabeçalhos
`List-Unsubscribe` e `List-Unsubscribe-Post`, que fazem o Gmail e outros mostrarem o botão "cancelar inscrição".

## Rodar localmente

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Sobe com H2 em `data/notification`. Sem `RESEND_API_KEY` nada é enviado: cada e-mail aparece no log com o
destinatário, o assunto e o link de descadastro. Ou suba tudo junto com `docker compose up --build` na raiz
do repositório.

```bash
curl -X POST localhost:8084/api/subscriptions -H "Content-Type: application/json" \
  -d '{"email": "maria@example.com", "productId": 7, "type": "PROMOTION", "consent": true}'
curl -X POST localhost:8084/api/product-events -H "Content-Type: application/json" \
  -d '{"type": "PROMOTION", "productId": 7, "productName": "Corrida dos Sapos", "productUrl": "http://localhost:3000/jogos/corrida-dos-sapos", "price": 79.90, "originalPrice": 99.90}'
# abra no navegador o link de descadastro que apareceu no log
```

Testes: `.\mvnw.cmd test`

## Variáveis de ambiente

| Variável | Exemplo |
|---|---|
| `NOTIFICATION_SERVICE_API_KEY` | chave longa aleatória, a mesma configurada no backend. Vazia = sem conferência (só local) |
| `PUBLIC_URL` | endereço público **deste serviço**; é ele que vai nos links de descadastro. Padrão `http://localhost:8084` |
| `STORE_URL` | endereço da loja, para o botão "voltar para a loja". Padrão `http://localhost:3000` |
| `RESEND_API_KEY` | chave do Resend. Vazia = os e-mails só aparecem no log |
| `EMAIL_FROM` | `ASOBI <avisos@asobi.com.br>`; o domínio precisa estar verificado no Resend |
| `EMAIL_REPLY_TO` | e-mail de atendimento, opcional |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (perfil `supabase`) |
| `DB_USER` / `DB_PASSWORD` | usuário próprio deste serviço no Supabase (abaixo) |
| `DB_SCHEMA` | opcional; padrão `notification` |

O intervalo mínimo entre dois avisos iguais (24 h) fica em `notification.cooldown` no `application.yml`.

### Usuário e esquema no Supabase

O serviço usa o mesmo projeto Supabase da loja (para ficar no free tier), mas com esquema e usuário
separados, sem acesso às tabelas do backend. No SQL Editor do Supabase, uma única vez:

```sql
CREATE SCHEMA notification;
CREATE ROLE notification_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA notification TO notification_service;
ALTER ROLE notification_service SET search_path = notification;
```

Pelo Session pooler, o `DB_USER` fica `notification_service.<id-do-projeto>`. A tabela `subscriptions` é
criada pelo Flyway na primeira subida.

## Deploy no Cloud Run

De dentro de `notification-service/`:

```bash
gcloud run deploy asobi-notification-service \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 1 \
  --set-env-vars "STORE_URL=https://asobi.com.br,EMAIL_FROM=ASOBI <avisos@asobi.com.br>" \
  --set-secrets NOTIFICATION_SERVICE_API_KEY=asobi-notification-api-key:latest,RESEND_API_KEY=asobi-resend-api-key:latest,DB_URL=asobi-db-url:latest,DB_USER=asobi-notification-db-user:latest,DB_PASSWORD=asobi-notification-db-password:latest
```

O primeiro deploy mostra a URL do serviço. Defina-a em `PUBLIC_URL` e atualize
(`gcloud run services update asobi-notification-service --update-env-vars PUBLIC_URL=<url>`): sem isso os
links de descadastro apontam para `localhost`.

(`--allow-unauthenticated` é necessário aqui: a página de descadastro é aberta pelo navegador de quem recebeu
o e-mail. A API continua protegida pela `X-Api-Key`.)

### Limite do Resend

O plano gratuito envia 100 e-mails por dia, somados aos de pedido e aos códigos do `two-factor-service`. Um
evento com muitos inscritos pode esbarrar nisso: o lote é recusado, a resposta traz `failed` e ninguém é
marcado como avisado. Os e-mails saem em lotes de até 100, uma chamada por lote.
