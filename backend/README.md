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

### Rodar localmente contra o Supabase

1. Copie `.env.example` para `.env` (já ignorado pelo Git) e preencha `DB_URL` e `DB_PASSWORD`
   (Supabase → **Connect** → *Session pooler*; a senha é a definida ao criar o projeto, ou em
   *Project Settings → Database → Reset database password*).
2. Rode com os perfis `local,supabase` — banco do Supabase, mas com o login de desenvolvimento do painel:

```powershell
$env:SPRING_PROFILES_ACTIVE="local,supabase"; .\mvnw.cmd spring-boot:run
```

Na primeira subida o Flyway cria as tabelas e o catálogo inicial no Supabase. O callback
`db/postgresql/afterMigrate.sql` liga o RLS em todas as tabelas, fechando a API REST automática do
Supabase (a chave `anon` é pública); o backend conecta como dono das tabelas e não é afetado.

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
| `STORE_URL` | `https://asobi.com.br` (loja; retorno do pagamento e links do painel) |
| `PUBLIC_URL` | URL pública deste backend no Cloud Run (monta o webhook do Mercado Pago) |
| `MERCADO_PAGO_ACCESS_TOKEN` | `TEST-...` (sandbox) ou `APP_USR-...` (produção). Vazio = modo manual |
| `MERCADO_PAGO_WEBHOOK_SECRET` | "assinatura secreta" gerada ao configurar Webhooks no painel do Mercado Pago |
| `SHIPPING_SERVICE_URL` | URL do microsserviço de frete ([`../shipping-service`](../shipping-service/README.md)); local: `http://localhost:8081`. Vazio = frete "a combinar" |
| `SHIPPING_SERVICE_API_KEY` | a mesma chave configurada no `shipping-service` (cabeçalho `X-Api-Key`) |
| `SUPABASE_URL` | `https://<id-do-projeto>.supabase.co` (fotos de produto). Vazio = pasta local, que **some** a cada nova instância do Cloud Run |
| `SUPABASE_SERVICE_KEY` | chave **secreta** do Supabase (Project Settings → API Keys → `service_role` / `sb_secret_...`). Nunca a `anon` |
| `SUPABASE_STORAGE_BUCKET` | opcional; padrão `product-images` |

Nunca commitar essas credenciais.

## Painel admin

`/admin` — Thymeleaf, renderizado pelo backend. Só entra quem faz login com Google **e** tem o
e-mail cadastrado em `/admin/admins` (conferido no banco a cada requisição). A sessão fica no
banco (Spring Session JDBC), então sobrevive ao Cloud Run desligar a instância.

- **Local**: tela de login tem um "modo desenvolvimento" que entra só com o e-mail
  (admin inicial `priscila@asobi.com.br`). Nunca ligado em produção (`asobi.admin.dev-login-enabled`).
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
  --set-env-vars FRONTEND_ORIGINS=https://asobi.com.br,STORE_URL=https://asobi.com.br,PUBLIC_URL=<url-do-cloud-run>,ADMIN_BOOTSTRAP_EMAIL=<email-google-da-priscila>,SHIPPING_SERVICE_URL=<url-do-shipping-service> \
  --set-secrets DB_URL=asobi-db-url:latest,DB_USER=asobi-db-user:latest,DB_PASSWORD=asobi-db-password:latest,GOOGLE_CLIENT_ID=asobi-google-client-id:latest,GOOGLE_CLIENT_SECRET=asobi-google-client-secret:latest,MERCADO_PAGO_ACCESS_TOKEN=asobi-mp-token:latest,MERCADO_PAGO_WEBHOOK_SECRET=asobi-mp-webhook-secret:latest,SHIPPING_SERVICE_API_KEY=asobi-shipping-api-key:latest,SUPABASE_SERVICE_KEY=asobi-supabase-service-key:latest
```

Acrescente em `--set-env-vars`: `SUPABASE_URL=https://<id-do-projeto>.supabase.co`.

`--min-instances 0` mantém o custo perto de zero (o primeiro acesso após um período parado leva alguns segundos para acordar).

## Pagamentos (Mercado Pago Checkout Pro)

1. `POST /api/orders` calcula o total com os preços do banco, confere o estoque e cria a
   preferência no Mercado Pago (`external_reference` = id público do pedido). A loja redireciona
   o cliente para o `checkoutUrl` (Pix, cartão, boleto — nenhum dado de cartão passa pela ASOBI).
2. O Mercado Pago avisa em `POST /api/webhooks/mercadopago` (assinatura `x-signature` conferida
   com `MERCADO_PAGO_WEBHOOK_SECRET`). O backend **consulta o pagamento na API** do Mercado Pago —
   nunca confia no corpo da notificação — e marca o pedido como pago, baixando o estoque uma única vez.
3. Quando o cliente volta para `/pedido/<id>?payment_id=...`, a loja chama
   `POST /api/orders/<id>/payment-sync`, que faz a mesma consulta (útil em localhost, onde o webhook não chega).

Configuração no [painel do Mercado Pago](https://www.mercadopago.com.br/developers/panel/app):
Webhooks → URL `https://<url-do-cloud-run>/api/webhooks/mercadopago`, evento **Pagamentos**.
Sem `MERCADO_PAGO_ACCESS_TOKEN`, a loja funciona em **modo manual**: o cliente vê o pedido e a Priscila
confirma o pagamento no painel (também serve para Pix direto).

## Frete (microsserviço shipping-service, com liga/desliga)

Em `/admin/configuracoes` a Priscila liga ou desliga o **frete automático**, define o CEP de origem,
o valor para frete grátis (a opção mais barata sai por R$ 0) e as medidas da caixa padrão.

- **Desligado** (padrão): o checkout mostra "Frete a combinar".
- **Ligado**: a loja chama `POST /api/shipping/quote` ao digitar o CEP e o cliente escolhe PAC/SEDEX etc.
  Ao fechar o pedido, o backend **cota de novo** e cobra o preço do servidor para a opção escolhida.

A cotação em si é feita pelo microsserviço [`shipping-service`](../shipping-service/README.md) (porta 8081),
que guarda o token do Melhor Envio. O backend manda só origem, destino e volumes (`ShippingServiceClient`)
e aplica aqui as regras da loja (toggle e frete grátis). Só dá para ligar com o serviço no ar
(`SHIPPING_SERVICE_URL`) e com `MELHOR_ENVIO_TOKEN` configurado **nele** (token gerado em Melhor Envio →
Integrações → Tokens de acesso, com permissão `shipping-calculate`).

## Fotos de produto (Supabase Storage)

No formulário de produto (`/admin/produtos`), a Priscila envia até **6 fotos** (JPG, PNG ou WebP, 5 MB cada).
A primeira é a capa; dá para trocar a capa e excluir fotos na tela de edição. Produto sem foto continua
mostrando o emoji.

- O navegador **reduz a foto antes de enviar** (lado maior de 1600 px, JPG): o envio fica rápido e as fotos
  gastam pouco do free tier do Supabase (1 GB de arquivos, 5 GB de tráfego por mês).
- O backend confere o tipo pelo conteúdo do arquivo (não pela extensão) e grava em
  `products/<id>/<uuid>.<ext>` num bucket **público**; a loja carrega a foto direto do Supabase.
- O bucket `product-images` é criado sozinho no primeiro envio. Em Storage → Buckets dá para conferir.
- **Local**: sem `SUPABASE_URL`/`SUPABASE_SERVICE_KEY`, os arquivos vão para `backend/data/uploads`
  (servidos em `/uploads/**`). Para testar o Supabase localmente, preencha as duas no `.env`.

A `SUPABASE_SERVICE_KEY` dá acesso total ao projeto: fica só no backend (Secret Manager), nunca no frontend.

## Estrutura

```
src/main/java/br/com/asobi/
  config/     configurações transversais (CORS, segurança, Mercado Pago)
  common/     status/health, tratamento de erros
  <domínio>/  catalog, review, order, payment, content, admin, shipping, settings
              cada um com controller / service / repository / model / dto
  storage/       onde as fotos ficam (Supabase Storage ou pasta local)
src/main/resources/
  db/migration/  migrações Flyway (V1, V2, ...)
  templates/     páginas Thymeleaf (painel admin)
```
