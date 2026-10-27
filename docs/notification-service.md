# ASOBI — notification-service (serviço de avisos de produto)

Manda e-mail para quem clicou em "avise-me" na página de um jogo. Os avisos são:

- **entrou em promoção** (`PROMOTION`);
- **voltou ao estoque** (`BACK_IN_STOCK`);
- **está acabando** (`LOW_STOCK`).

Ele guarda quem pediu cada aviso. Quando o backend conta que algo mudou num jogo, ele manda os e-mails,
cada um com um link para cancelar o aviso.

Só o backend da loja ([`backend`](backend.md)) chama as rotas `/api`. O navegador do cliente só abre a página de cancelamento.

- Java 17, Spring Boot 4.1, JPA (acesso ao banco), Flyway (cria as tabelas) e Thymeleaf (monta o e-mail e a página de cancelamento)
- Banco: um esquema só dele no Supabase, chamado `notification` (no computador usa o H2, um banco em arquivo)
- Roda na porta **8084** no computador
- No ar, fica no Google Cloud Run (serviço `asobi-notification-service`), que desliga sozinho quando ninguém usa

## O que ele faz e o que não faz

| Faz | Não faz (isso é do backend) |
|---|---|
| Guardar os pedidos de aviso (e-mail + jogo + tipo), só se a pessoa concordou | Mostrar o formulário "avise-me" na página do jogo |
| Mandar os e-mails pelo Resend quando chega uma novidade | Perceber que o preço ou o estoque mudou |
| Não mandar o mesmo aviso para a mesma pessoa duas vezes em 24 horas | Saber o que é um jogo: nome, preço e link chegam prontos |
| Cancelar o aviso pelo link do e-mail, sem precisar de login | Mandar os e-mails de pedido e a newsletter |
| Apagar tudo de um e-mail (LGPD) ou de um jogo que saiu da loja | |

### Dados pessoais (LGPD)

- Guarda **só o e-mail do adulto** que pediu o aviso e o número do jogo. Nenhum dado de criança.
- Só aceita o pedido com `consent: true` (a pessoa concordou), e grava a data em `consented_at`.
  Na loja, a caixa de "concordo" precisa vir **desmarcada**.
- Todo e-mail tem o link para cancelar, que apaga o pedido na hora (não fica só "desativado").
- Para apagar tudo de alguém: `DELETE /api/subscriptions?email=`. Para ver o que tem de alguém: `GET /api/subscriptions?email=`.

## Rotas

Toda rota que começa com `/api/` exige o cabeçalho `X-Api-Key` com a mesma senha de `NOTIFICATION_SERVICE_API_KEY`.

| Rota | O que faz |
|---|---|
| `POST /api/subscriptions` | Cria um pedido de aviso. Recebe `{"email": "...", "productId": 7, "type": "PROMOTION", "consent": true}`. Responde `201` se for novo ou `200` se a pessoa já tinha pedido, com `{id, email, productId, type, consentedAt, lastNotifiedAt}` |
| `GET /api/subscriptions?email=&productId=` | Lista os pedidos, do mais novo para o mais antigo. Os dois filtros são opcionais |
| `DELETE /api/subscriptions/{id}` | Apaga um pedido (`204`) |
| `DELETE /api/subscriptions?email=` | Apaga tudo de um e-mail. Responde `{"removed": 2}` |
| `DELETE /api/subscriptions?productId=` | Apaga tudo de um jogo (quando ele sai da loja) |
| `POST /api/product-events` | O backend avisa que um jogo mudou (exemplo abaixo). Responde `{subscribers, sent, failed, skipped}` |
| `GET /actuator/health` | Diz se o serviço está no ar. Não precisa de senha |

O `type` pode ser `PROMOTION`, `BACK_IN_STOCK` ou `LOW_STOCK`, tanto no pedido quanto na novidade.

Exemplo do que o backend manda quando um jogo muda:

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

- `price` é o preço de agora.
- `originalPrice` é opcional; se for maior que `price`, aparece riscado no e-mail.
- `stock` é opcional; no `LOW_STOCK`, vira a frase "Restam só N unidades".

Na resposta:

- `skipped` = quantas pessoas já tinham recebido esse aviso nas últimas 24 horas;
- `failed` = quantas não receberam porque o Resend recusou o envio. Essas pessoas não ficam marcadas
  como avisadas, então é só o backend mandar a novidade de novo.

Quando dá erro, a resposta vem em JSON com a explicação:
`400` dados errados ou sem concordância · `401` senha errada · `404` pedido não encontrado.

### Página para cancelar o aviso (aberta, sem senha)

| Rota | O que faz |
|---|---|
| `GET /descadastro/{token}` | Mostra a página perguntando se quer cancelar. Abrir o link **não** cancela nada, porque antivírus e programas de e-mail costumam abrir links sozinhos |
| `POST /descadastro/{token}` | Cancela aquele aviso. Com `all=true`, cancela todos os avisos daquele e-mail |

Quem garante que é a pessoa certa é o `token` do link: um código aleatório e enorme (256 bits), diferente
para cada pedido. Se o token não existir mais, a página mostra "aviso cancelado" do mesmo jeito, para o
link poder ser aberto mais de uma vez sem erro.

Os e-mails também levam os cabeçalhos `List-Unsubscribe` e `List-Unsubscribe-Post`, que fazem o Gmail
e outros mostrarem o botão "cancelar inscrição" lá em cima.

## Como rodar no computador

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Ele sobe com o banco H2 na pasta `data/notification`. Sem `RESEND_API_KEY`, nenhum e-mail é enviado:
cada e-mail aparece no log (a saída do terminal) com o destinatário, o assunto e o link de cancelamento.
Outra opção é subir tudo junto com `docker compose up --build` na pasta principal do projeto.

```bash
curl -X POST localhost:8084/api/subscriptions -H "Content-Type: application/json" \
  -d '{"email": "maria@example.com", "productId": 7, "type": "PROMOTION", "consent": true}'
curl -X POST localhost:8084/api/product-events -H "Content-Type: application/json" \
  -d '{"type": "PROMOTION", "productId": 7, "productName": "Corrida dos Sapos", "productUrl": "http://localhost:3000/jogos/corrida-dos-sapos", "price": 79.90, "originalPrice": 99.90}'
# abra no navegador o link de cancelamento que apareceu no log
```

Testes: `.\mvnw.cmd test`

## Configurações (variáveis de ambiente)

| Variável | O que colocar |
|---|---|
| `NOTIFICATION_SERVICE_API_KEY` | uma senha longa e aleatória, igual à do backend. Se ficar vazia, não confere a senha (só use assim no computador) |
| `PUBLIC_URL` | endereço **deste serviço** na internet; é ele que vai nos links de cancelamento. Se não preencher, usa `http://localhost:8084` |
| `STORE_URL` | endereço da loja, para o botão "voltar para a loja". Se não preencher, usa `http://localhost:3000` |
| `RESEND_API_KEY` | chave do Resend. Vazia = os e-mails só aparecem no log |
| `EMAIL_FROM` | `ASOBI <avisos@asobi.com.br>`; o domínio precisa estar verificado no Resend |
| `EMAIL_REPLY_TO` | e-mail de atendimento para respostas (opcional) |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (com o perfil `supabase`) |
| `DB_USER` / `DB_PASSWORD` | usuário e senha deste serviço no Supabase (veja abaixo) |
| `DB_SCHEMA` | opcional; se não preencher, usa `notification` |

O tempo mínimo entre dois avisos iguais (24 horas) fica em `notification.cooldown` no `application.yml`.

### Criando o usuário e o esquema no Supabase

O serviço usa o mesmo projeto do Supabase que a loja (para continuar no plano grátis), mas com um
esquema e um usuário separados, sem acesso às tabelas do backend. No SQL Editor do Supabase, uma vez só:

```sql
CREATE SCHEMA notification;
CREATE ROLE notification_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA notification TO notification_service;
ALTER ROLE notification_service SET search_path = notification;
```

Pelo Session pooler do Supabase, o `DB_USER` fica `notification_service.<id-do-projeto>`.
A tabela `subscriptions` é criada sozinha pelo Flyway na primeira vez que o serviço sobe.

## Colocar no ar (Cloud Run)

Dentro da pasta `notification-service/`:

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

Na primeira vez, o comando mostra o endereço do serviço. Coloque esse endereço em `PUBLIC_URL`:

```bash
gcloud run services update asobi-notification-service --update-env-vars PUBLIC_URL=<endereço>
```

Sem isso, os links de cancelamento apontam para `localhost` e não funcionam.

Aqui o `--allow-unauthenticated` é obrigatório, porque a página de cancelamento é aberta pelo navegador
de quem recebeu o e-mail. As rotas `/api` continuam protegidas pela senha do `X-Api-Key`.

### Limite do Resend

O plano grátis manda 100 e-mails por dia, contando junto com os e-mails de pedido e os códigos do
`two-factor-service`. Se um jogo tiver muitos interessados, pode passar do limite: o Resend recusa,
a resposta mostra `failed` e ninguém fica marcado como avisado. Os e-mails saem em grupos de até 100,
um envio por grupo.
