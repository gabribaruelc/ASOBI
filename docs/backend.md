# ASOBI — backend (Java / Spring Boot)

É o "cérebro" da loja. Ele faz duas coisas:

1. **Responde a loja** ([`frontend`](frontend.md)): manda a lista de jogos, recebe pedidos,
   confere pagamentos etc. Tudo em JSON, pelas rotas `/api/...`.
2. **Mostra o painel do lojista** (`/admin`): as telas são montadas aqui mesmo, com Thymeleaf.

- Java 17, Spring Boot 4.1, Spring Data JPA (acesso ao banco), Flyway (cria e atualiza as tabelas) e Thymeleaf (telas)
- Banco: Supabase (PostgreSQL) quando está no ar; no computador usa o H2, um banco em arquivo que imita o PostgreSQL
- No ar, fica no Google Cloud Run (a imagem é feita pelo `Dockerfile`)

## Como rodar no computador

Não precisa instalar o Maven nem um banco: o `mvnw` baixa o Maven sozinho e o perfil `local` usa o H2.

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

- http://localhost:8080/ — página que mostra se está tudo funcionando
- http://localhost:8080/api/health — responde `{"status":"ok"}`
- http://localhost:8080/h2-console — tela para olhar o banco local (JDBC URL: `jdbc:h2:file:./data/asobi`, usuário `sa`, sem senha)

Testes: `.\mvnw.cmd test`

> No Git Bash do Windows, use `mvnw.cmd`. O `mvnw` comum falha ao instalar o Maven por falta de permissão em `/tmp`.

### Rodar no computador usando o banco do Supabase

1. Copie `.env.example` para `.env` (o Git já ignora esse arquivo) e preencha `DB_URL` e `DB_PASSWORD`.
   Esses dados ficam no Supabase em **Connect** → *Session pooler*. A senha é a que foi criada junto com o
   projeto (ou dá para trocar em *Project Settings → Database → Reset database password*).
2. Rode com os perfis `local,supabase`. Assim usa o banco do Supabase, mas mantém o login simplificado do painel:

```powershell
$env:SPRING_PROFILES_ACTIVE="local,supabase"; .\mvnw.cmd spring-boot:run
```

Na primeira vez, o Flyway cria as tabelas e os jogos de exemplo no Supabase.

O arquivo `db/postgresql/afterMigrate.sql` liga o RLS (uma trava do PostgreSQL) em todas as tabelas.
Isso é importante porque o Supabase cria sozinho uma API pública para o banco, e a chave `anon` dele é pública.
Com o RLS ligado, ninguém de fora lê as tabelas por essa API. O backend não é afetado, porque entra como
dono das tabelas.

## Configurações (variáveis de ambiente, perfil `prod`)

| Variável | O que colocar |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (o Dockerfile já faz isso) |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (Supabase → Connect → **Session pooler**) |
| `DB_USER` | `postgres.<id-do-projeto>` |
| `DB_PASSWORD` | senha do banco no Supabase |
| `FRONTEND_ORIGINS` | endereços da loja que podem chamar a API: `https://asobi.com.br,https://www.asobi.com.br` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | dados do Google para o login do painel |
| `ADMIN_AUTH_SERVICE_URL` | endereço do `admin-auth-service` (guarda a lista de admins). Vazio = lista só na memória, para testes |
| `ADMIN_AUTH_SERVICE_API_KEY` | a mesma senha configurada no `admin-auth-service` |
| `ADMIN_BOOTSTRAP_EMAIL` | só é usado quando `ADMIN_AUTH_SERVICE_URL` está vazia; no ar, o primeiro admin é configurado no `admin-auth-service` |
| `STORE_URL` | `https://asobi.com.br` (endereço da loja, usado na volta do pagamento e nos links do painel) |
| `PUBLIC_URL` | endereço deste backend no Cloud Run (usado para montar o endereço que o Mercado Pago chama) |
| `MERCADO_PAGO_ACCESS_TOKEN` | `TEST-...` (testes) ou `APP_USR-...` (de verdade). Vazio = modo manual |
| `MERCADO_PAGO_WEBHOOK_SECRET` | a "assinatura secreta" que o Mercado Pago mostra quando você configura os Webhooks |
| `SHIPPING_SERVICE_URL` | endereço do [`shipping-service`](shipping-service.md) (frete); no computador: `http://localhost:8081`. Vazio = frete "a combinar" |
| `SHIPPING_SERVICE_API_KEY` | a mesma senha configurada no `shipping-service` (vai no cabeçalho `X-Api-Key`) |
| `TWO_FACTOR_SERVICE_URL` | endereço do [`two-factor-service`](two-factor-service.md); no computador: `http://localhost:8082`. Preenchida, o painel pede um código por e-mail depois do login. Vazia = só o login |
| `TWO_FACTOR_SERVICE_API_KEY` | a mesma senha configurada no `two-factor-service` |
| `NOTIFICATION_SERVICE_URL` | endereço do [`notification-service`](notification-service.md); no computador: `http://localhost:8084`. Preenchida, a página do jogo mostra o "avise-me". Vazia = avisos desligados |
| `NOTIFICATION_SERVICE_API_KEY` | a mesma senha configurada no `notification-service` |
| `SUPABASE_URL` | `https://<id-do-projeto>.supabase.co` (fotos dos jogos). Vazio = as fotos vão para uma pasta local, que **some** sempre que o Cloud Run reinicia |
| `SUPABASE_SERVICE_KEY` | a chave **secreta** do Supabase (Project Settings → API Keys → `service_role` / `sb_secret_...`). Nunca a `anon`. Também serve para conferir o login do cliente |
| `SUPABASE_STORAGE_BUCKET` | opcional; se não preencher, usa `product-images` |
| `RESEND_API_KEY` | chave do [Resend](https://resend.com) (`re_...`). Vazia = os e-mails só aparecem no log |
| `EMAIL_FROM` | quem envia, ex.: `ASOBI <pedidos@asobi.com.br>` (o domínio precisa estar verificado no Resend) |
| `EMAIL_REPLY_TO` | opcional; e-mail que recebe as respostas dos clientes |
| `JOBS_API_KEY` | senha do cabeçalho `X-Job-Key`, usada pelas tarefas automáticas do Cloud Scheduler (lembrete de pagamento). Vazia = tarefas desligadas |
| `NEWSLETTER_DAILY_LIMIT` | opcional; máximo de e-mails de newsletter a cada 24 horas (padrão `80`, para sobrar espaço para os e-mails de pedido no plano grátis do Resend) |

Nunca coloque essas senhas no Git.

## Painel do lojista (`/admin`)

As telas são feitas com Thymeleaf e montadas pelo backend. Para entrar, a pessoa precisa fazer login com
o Google **e** ter o e-mail na lista de admins (`/admin/admins`).

Quem guarda essa lista e confere o login é o [`admin-auth-service`](admin-auth-service.md).
A cada clique no painel, o backend pergunta a ele se o e-mail ainda é admin (e guarda a resposta por
30 segundos). O login fica salvo no banco (Spring Session JDBC), então a pessoa não é deslogada quando o
Cloud Run desliga e liga de novo.

### Login

- **No computador**, o backend procura o `admin-auth-service` em `http://localhost:8083`. Então ligue os dois
  (`.\mvnw.cmd spring-boot:run` na pasta dele). Os admins ficam salvos em `admin-auth-service/data/` e
  continuam lá depois de reiniciar. Se o serviço estiver desligado, ninguém entra no painel.
- **Com `ADMIN_AUTH_SERVICE_URL` vazia** (`ADMIN_AUTH_SERVICE_URL=` no `.env`), a lista fica só na memória,
  com o `ADMIN_BOOTSTRAP_EMAIL`, e o que for adicionado some quando reinicia. É assim que os testes automáticos rodam.
- **Modo desenvolvimento**: no computador, a tela de login tem uma opção que entra só com o e-mail
  (admin inicial `admin@asobi.com.br`). Isso nunca fica ligado no ar (`asobi.admin.dev-login-enabled`).
- **Google (no ar)**: no [Google Cloud Console](https://console.cloud.google.com/apis/credentials), crie um
  "ID do cliente OAuth" do tipo *Aplicativo da Web* com o endereço de retorno
  `https://<url-do-cloud-run>/login/oauth2/code/google` (e `http://localhost:8080/login/oauth2/code/google`
  para testar no computador com `SPRING_PROFILES_ACTIVE=local,google`).
- **Código por e-mail**: com `TWO_FACTOR_SERVICE_URL` preenchida, depois do login o painel manda um código de
  6 números para o e-mail do admin e só abre depois que ele digita o código em `/admin/verificacao`.
  Quem cria, envia e confere o código é o [`two-factor-service`](two-factor-service.md).
  Se ele estiver fora do ar, ninguém passa dessa tela. No computador, sem `RESEND_API_KEY` no serviço,
  o código aparece no log dele. Vale também para o modo desenvolvimento.

### O que tem no painel

- **Dashboard** (`/admin`): números dos últimos 7, 30 ou 90 dias: quanto vendeu (pela data do pagamento),
  pedidos pagos, valor médio por pedido, vendas por dia, jogos mais vendidos e mais vistos.
  As visualizações são só um contador por jogo e por dia (`product_views`), que sobe quando alguém abre a
  página do jogo. Não guarda quem viu.
- **Políticas** (`/admin/politicas`) e **Contato e rodapé** (`/admin/contato`): textos das páginas de
  Privacidade, Trocas e Frete, e telefone/WhatsApp, e-mail, redes sociais, razão social e CNPJ da loja.
- **Avisos de produto ("avise-me")**: com `NOTIFICATION_SERVICE_URL` preenchida, a página do jogo mostra o
  formulário "avise-me" (`POST /api/product-alerts`). Quando o lojista coloca um jogo em promoção ou repõe um
  jogo esgotado, o backend avisa o [`notification-service`](notification-service.md), que manda os
  e-mails. Apagar um jogo apaga os pedidos de aviso dele. O aviso de "está acabando" (`LOW_STOCK`) sai quando
  uma venda leva o jogo ao limite de estoque baixo (próximo item).
- **Estoque baixo**: o limite fica em `/admin/configuracoes` (padrão 3; 0 desliga). Quando um pedido pago deixa
  um jogo com esse número de unidades ou menos:
  - os admins recebem o e-mail "Estoque baixo: <jogo>" (ou "Esgotou: <jogo>");
  - quem pediu o aviso de "está acabando" recebe o e-mail;
  - a loja mostra "Últimas unidades" (campo `lowStock` na API);
  - o dashboard mostra o jogo na lista.

  O aviso sai uma vez só, na venda que passa do limite, e não a cada venda depois disso.
- **Newsletter** (`/admin/newsletter`):
  - O lojista escreve o e-mail (assunto, texto e um botão opcional), salva o rascunho, vê como vai ficar,
    manda um teste para si mesmo e depois envia para todos.
  - Cada e-mail leva um link para a pessoa sair da lista (`/newsletter/descadastro/{token}`, página do
    próprio backend) e o cabeçalho `List-Unsubscribe`. Sair da lista apaga o e-mail.
  - O envio para quando chega em `NEWSLETTER_DAILY_LIMIT` e-mails em 24 horas. No dia seguinte, o lojista
    clica em "Continuar envio" e o resto é enviado, sem repetir ninguém.
  - Depois do primeiro envio, o texto não pode mais mudar, e só recebe quem já estava inscrito naquela hora.
  - Dá para colocar até 12 **jogos em destaque** (cartões com foto, preço "de/por" e link). O preço é lido na
    hora do envio, e jogo esgotado fica de fora.
  - O botão **"Anunciar promoções"** cria um rascunho pronto com os jogos em promoção que têm estoque.
  - A lista de inscritos pode ser baixada em CSV (planilha).

## Colocar no ar (Cloud Run)

Antes de colocar qualquer serviço no ar pela primeira vez, crie as senhas no Secret Manager (o cofre de
senhas do Google). Na pasta principal do projeto: `bash scripts/create-secrets.sh`. O script cria as senhas
internas sozinho, copia as do banco dos arquivos `.env` e pergunta as das contas externas (Enter pula; dá
para rodar de novo depois).

Com o [gcloud CLI](https://cloud.google.com/sdk/docs/install) instalado e logado, dentro da pasta `backend/`:

```bash
gcloud run deploy asobi-backend \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 2 \
  --set-env-vars FRONTEND_ORIGINS=https://asobi.com.br,STORE_URL=https://asobi.com.br,PUBLIC_URL=<url-do-cloud-run>,SHIPPING_SERVICE_URL=<url-do-shipping-service>,ADMIN_AUTH_SERVICE_URL=<url-do-admin-auth-service>,TWO_FACTOR_SERVICE_URL=<url-do-two-factor-service>,NOTIFICATION_SERVICE_URL=<url-do-notification-service> \
  --set-secrets DB_URL=asobi-db-url:latest,DB_USER=asobi-db-user:latest,DB_PASSWORD=asobi-db-password:latest,GOOGLE_CLIENT_ID=asobi-google-client-id:latest,GOOGLE_CLIENT_SECRET=asobi-google-client-secret:latest,MERCADO_PAGO_ACCESS_TOKEN=asobi-mp-token:latest,MERCADO_PAGO_WEBHOOK_SECRET=asobi-mp-webhook-secret:latest,SHIPPING_SERVICE_API_KEY=asobi-shipping-api-key:latest,ADMIN_AUTH_SERVICE_API_KEY=asobi-admin-auth-api-key:latest,TWO_FACTOR_SERVICE_API_KEY=asobi-two-factor-api-key:latest,NOTIFICATION_SERVICE_API_KEY=asobi-notification-api-key:latest,SUPABASE_SERVICE_KEY=asobi-supabase-service-key:latest,RESEND_API_KEY=asobi-resend-api-key:latest,JOBS_API_KEY=asobi-jobs-api-key:latest
```

Também coloque em `--set-env-vars`: `SUPABASE_URL=https://<id-do-projeto>.supabase.co` e
`EMAIL_FROM=ASOBI <pedidos@asobi.com.br>` (como esse valor tem espaço, coloque a lista inteira entre aspas).

O `--min-instances 0` deixa o custo perto de zero: o serviço desliga quando ninguém usa. A desvantagem é
que o primeiro acesso depois de um tempo parado demora alguns segundos.

## Pagamento (Mercado Pago Checkout Pro)

Passo a passo de uma compra:

1. A loja chama `POST /api/orders`. O backend calcula o total com os preços do banco (nunca com os que vêm
   do navegador), confere o estoque e cria o pagamento no Mercado Pago (o campo `external_reference` leva o
   número do pedido). A loja manda o cliente para o `checkoutUrl`, onde ele paga com Pix, cartão ou boleto.
   Nenhum dado de cartão passa pela ASOBI.
2. Quando o pagamento muda, o Mercado Pago chama `POST /api/webhooks/mercadopago`. O backend confere a
   assinatura (`x-signature`) com `MERCADO_PAGO_WEBHOOK_SECRET` e **pergunta de novo ao Mercado Pago** como
   está o pagamento, em vez de acreditar no que veio na chamada. Se estiver aprovado, marca o pedido como pago
   e tira os jogos do estoque (uma vez só).
3. Quando o cliente volta para `/pedido/<id>?payment_id=...`, a loja chama
   `POST /api/orders/<id>/payment-sync`, que faz a mesma pergunta. Isso ajuda no computador, onde o
   Mercado Pago não consegue chamar o webhook.

No [painel do Mercado Pago](https://www.mercadopago.com.br/developers/panel/app):
Webhooks → URL `https://<url-do-cloud-run>/api/webhooks/mercadopago`, evento **Pagamentos**.

Sem `MERCADO_PAGO_ACCESS_TOKEN`, a loja funciona em **modo manual**: o cliente faz o pedido e o lojista marca
como pago no painel (serve também para quem recebe Pix direto).

## Frete (com botão de liga/desliga)

Em `/admin/configuracoes` o lojista liga ou desliga o **frete automático**, coloca o CEP de onde os pacotes
saem, o valor a partir do qual o frete é grátis (a opção mais barata fica R$ 0) e as medidas da caixa padrão.

- **Desligado** (padrão): o checkout mostra "Frete a combinar".
- **Ligado**: quando o cliente digita o CEP, a loja chama `POST /api/shipping/quote` e ele escolhe PAC, SEDEX etc.
  Ao fechar o pedido, o backend **calcula de novo** e cobra o preço que ele mesmo calculou.

Quem faz a conta é o [`shipping-service`](shipping-service.md) (porta 8081), que guarda o token do
Melhor Envio. O backend manda só os CEPs e os pacotes (pelo `ShippingServiceClient`) e cuida das regras da
loja (liga/desliga e frete grátis). O botão só pode ser ligado com o serviço no ar (`SHIPPING_SERVICE_URL`)
e com o `MELHOR_ENVIO_TOKEN` configurado **nele** (o token é gerado em Melhor Envio → Integrações → Tokens
de acesso, com a permissão `shipping-calculate`).

## Fotos dos jogos (Supabase Storage)

Na tela de produto (`/admin/produtos`), o lojista envia até **6 fotos** (JPG, PNG ou WebP, até 5 MB cada).
A primeira é a capa; dá para trocar a capa e apagar fotos na tela de edição. Jogo sem foto mostra o emoji.

- O navegador **diminui a foto antes de enviar** (no máximo 1600 px no lado maior, em JPG). Assim o envio é
  rápido e as fotos gastam pouco do plano grátis do Supabase (1 GB de arquivos e 5 GB de download por mês).
- O backend confere o tipo da foto olhando o conteúdo do arquivo (e não só a extensão) e salva em
  `products/<id>/<uuid>.<ext>` numa pasta **pública**. A loja mostra a foto direto do Supabase.
- A pasta `product-images` é criada sozinha no primeiro envio. Dá para ver em Storage → Buckets.
- **No computador**: sem `SUPABASE_URL`/`SUPABASE_SERVICE_KEY`, as fotos vão para `backend/data/uploads`
  (e aparecem em `/uploads/**`). Para testar com o Supabase, preencha as duas no `.env`.

A `SUPABASE_SERVICE_KEY` abre tudo do projeto. Ela fica só no backend (no Secret Manager), nunca na loja.

## Lembrete de pagamento (carrinho abandonado)

Quem fez o pedido mas não foi até o pagamento recebe **um** lembrete, entre 2 e 48 horas depois
(`asobi.payment-reminders`), com o link do pedido. Na página do pedido, o botão **"Pagar agora"**
(`POST /api/orders/{id}/checkouts`) confere o estoque de novo e abre um novo pagamento no Mercado Pago,
com o preço do pedido.

Não recebem lembrete: quem já gerou Pix ou boleto (para não pagar duas vezes), pedidos com algum jogo
esgotado e a loja em modo manual (sem Mercado Pago).

O Cloud Run não fica ligado o tempo todo, então quem dispara o lembrete é o **Cloud Scheduler** (um
"despertador" do Google, com 3 tarefas grátis), de hora em hora, mandando a senha `JOBS_API_KEY` no
cabeçalho. Depois de colocar o backend no ar, na pasta principal do projeto:
`bash scripts/create-scheduler-job.sh <url-do-cloud-run>` (o script pega a senha do Secret Manager).
É o mesmo que rodar:

```bash
gcloud scheduler jobs create http asobi-payment-reminders   --location southamerica-east1   --schedule "0 * * * *" --time-zone "America/Sao_Paulo"   --uri "https://<url-do-cloud-run>/api/jobs/payment-reminders"   --http-method POST   --headers "X-Job-Key=<a mesma JOBS_API_KEY do backend>"
```

Sem `JOBS_API_KEY`, a rota responde `404` (como se não existisse). No computador dá para rodar na mão:
`curl -X POST -H "X-Job-Key: <chave>" http://localhost:8080/api/jobs/payment-reminders`.

## E-mails de pedido (Resend)

O cliente recebe e-mail quando o pedido é **recebido**, quando o pagamento é **confirmado** e quando o pedido
é **enviado** (com o código de rastreio). Os admins recebem um aviso a cada pedido pago. O modelo do e-mail
fica em `templates/email/order.html`.

- O e-mail só sai depois que tudo foi salvo no banco. Se o Resend falhar, o pedido não é afetado (o erro
  fica no log).
- **Como configurar**: crie a conta no [Resend](https://resend.com), gere a chave em *API Keys* e verifique o
  domínio em *Domains* (o Resend mostra os registros DNS para cadastrar no registro.br). Enquanto o domínio
  não estiver verificado, o remetente de teste `onboarding@resend.dev` só entrega para o dono da conta.
- Plano grátis: 100 e-mails por dia, contando a newsletter. Cada pedido completo gasta 3 do cliente + 1 por
  admin. Por isso a newsletter para em `NEWSLETTER_DAILY_LIMIT` (80) por dia.
- **No computador**: sem `RESEND_API_KEY`, nada é enviado; o assunto e o destinatário aparecem no log.

## Conta do cliente (login com Google pelo Supabase)

O login é feito na loja, direto com o Supabase. Depois, a loja manda um token no cabeçalho
`Authorization: Bearer ...` e o backend pergunta ao Supabase se ele é válido (`GET /auth/v1/user`).

- `POST /api/orders`: o token é **opcional**. Com ele, o pedido fica ligado à conta
  (`orders.customer_user_id`). Sem ele, ou com token vencido, o pedido é feito sem cadastro normalmente.
- `GET /api/account/orders` ("Meus pedidos"): precisa do token (sem ele, responde `401`). Mostra os pedidos
  da conta e, se o e-mail da conta for confirmado, também os pedidos feitos sem cadastro com esse e-mail.
- Usa `SUPABASE_URL` e `SUPABASE_SERVICE_KEY` (as mesmas das fotos). Sem elas, o login de cliente fica
  desligado e a loja continua vendendo sem cadastro. Como ligar o Google no Supabase:
  [`frontend`](frontend.md).

## Como as pastas estão organizadas

```
src/main/java/br/com/asobi/
  config/     configurações gerais (CORS, segurança, Mercado Pago)
  common/     página de status e tratamento de erros
  <assunto>/  catalog (jogos), review (avaliações), order (pedidos), payment (pagamento),
              content (textos do site), admin (painel), shipping (frete), settings (configurações)
              cada pasta tem: controller (rotas) / service (regras) / repository (banco) /
              model (tabelas) / dto (o que entra e sai da API)
  storage/       onde as fotos são salvas (Supabase ou pasta local)
  notification/  e-mails de pedido (Resend)
  account/       conta do cliente (confere o login do Supabase)
src/main/resources/
  db/migration/  scripts do Flyway que criam e mudam as tabelas (V1, V2, ...)
  templates/     telas do painel e modelos de e-mail (Thymeleaf)
```
