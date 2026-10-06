# ASOBI — loja (Next.js)

Loja da ASOBI vista pelo cliente: catálogo, ficha de produto, carrinho, checkout, acompanhamento
do pedido e "Meus pedidos". Todos os dados vêm da API do backend ([`../backend`](../backend/README.md));
o painel admin também fica lá (`/admin` aqui só redireciona para ele).

- Next.js 16 (App Router) · React 19 · CSS Modules
- Hospedagem: Cloudflare Workers, pelo [OpenNext](https://opennext.js.org/cloudflare)

## Rodar localmente

Suba o backend antes (porta 8080) e depois:

```bash
cp .env.example .env.local   # ajuste se o backend não estiver em localhost:8080
npm install
npm run dev                  # http://localhost:3000
```

`npm run lint` confere o código; `npm run build` gera a versão de produção.

## Variáveis de ambiente

| Variável | Para quê |
|---|---|
| `NEXT_PUBLIC_API_URL` | URL do backend. Padrão: `http://localhost:8080` |
| `NEXT_PUBLIC_SUPABASE_URL` | Login de cliente: `https://<id-do-projeto>.supabase.co` |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Login de cliente: chave **pública** do Supabase (`anon` / `publishable`), nunca a secreta |

Sem as duas variáveis do Supabase a loja funciona normalmente, só sem o link "Entrar"
(a compra sem cadastro continua valendo).

## Como as páginas buscam os dados

- **Catálogo no servidor** (`src/app/lib/catalog.js`): home, `/jogos`, `/novidades`, `/promocoes`, `/sobre`
  e a ficha `/jogos/[slug]` são Server Components. As respostas da API ficam em cache por **1 minuto**,
  então o que a Priscila muda no painel aparece na loja em até esse tempo. Cada ficha tem título e
  descrição próprios (SEO).
- **Carrinho e checkout no navegador** (`src/app/lib/useProducts.js`): buscam preço e estoque na hora.
- Se o backend não responder, as listas mostram um aviso em vez de quebrar a página (e o build).

## Login de cliente (Google, pelo Supabase Auth)

`/login` → "Continuar com Google" → volta para `/conta` ("Meus pedidos"). Não há senha nem tela de
cadastro: a conta nasce no primeiro login. No checkout, quem está logado tem o pedido ligado à conta;
pedidos antigos feitos sem cadastro com o mesmo e-mail também aparecem em "Meus pedidos".

Para ligar (uma vez só, no painel do Supabase):

1. **Authentication → Sign In / Providers → Google**: ative e cole o *Client ID* e o *Client Secret* de um
   "ID do cliente OAuth" do [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   (tipo *Aplicativo da Web*, com a URI de redirecionamento que o Supabase mostra nessa tela:
   `https://<id-do-projeto>.supabase.co/auth/v1/callback`).
2. **Authentication → URL Configuration**: em *Site URL* ponha o endereço da loja e, em *Redirect URLs*,
   `http://localhost:3000/**` e `https://<dominio-da-loja>/**`.
3. Preencha `NEXT_PUBLIC_SUPABASE_URL` e `NEXT_PUBLIC_SUPABASE_ANON_KEY` aqui, e `SUPABASE_URL` e
   `SUPABASE_SERVICE_KEY` no backend (é ele que confere o login).

## Deploy na Cloudflare

A loja precisa de um servidor para renderizar o catálogo, então vai para **Workers** (free tier), não
para o Pages estático. Configuração em `wrangler.jsonc` e `open-next.config.ts`.

```bash
npx wrangler login
npx wrangler r2 bucket create asobi-frontend-cache   # só na primeira vez
npm run deploy
```

- As variáveis `NEXT_PUBLIC_*` entram no build: defina-as em `.env.production` (ou no ambiente) **antes**
  de `npm run deploy`.
- O cache das páginas fica num bucket **R2** (10 GB e 1 milhão de gravações por mês no plano gratuito;
  a Cloudflare pede um cartão cadastrado para ativar o R2, sem cobrança dentro desse limite).
- `npm run preview` roda a versão da Cloudflare localmente. No Windows, o OpenNext recomenda usar o WSL
  se o build apresentar problemas.
