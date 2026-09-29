# ASOBI — loja (frontend)

Next.js (App Router). Mostra a loja para o cliente; os dados e as regras ficam no [backend](../backend/README.md).

## Rodar localmente

```bash
npm install
cp .env.example .env.local   # aponta para o backend em http://localhost:8080
npm run dev                  # http://localhost:3000
```

Sem `NEXT_PUBLIC_API_URL`, a loja roda no modo antigo (dados mockados + `localStorage`).

## Deploy no Cloudflare Workers (OpenNext)

A loja roda no **Cloudflare Workers** (plano gratuito, uso comercial liberado) através do adaptador
[OpenNext](https://opennext.js.org/cloudflare). O Cloudflare Pages não serve aqui porque a loja tem
páginas dinâmicas (`/jogos/[slug]`, `/pedido/[id]`) que precisam ser montadas no servidor.

- `wrangler.jsonc`: nome do Worker (`asobi-frontend`) e configuração do Cloudflare.
- `open-next.config.ts`: configuração do adaptador (sem cache R2, porque a loja não usa ISR).

`NEXT_PUBLIC_API_URL` é embutida **na hora do build**, então precisa estar definida no ambiente de build,
não só no Worker.

### Opção 1 — automático pelo GitHub (recomendado)
No painel da Cloudflare: *Workers & Pages → Create → Import a repository*, escolha este repositório e configure:

| Campo | Valor |
|---|---|
| Root directory | `frontend` |
| Build command | `npx opennextjs-cloudflare build` |
| Deploy command | `npx opennextjs-cloudflare deploy` |
| Variável de build | `NEXT_PUBLIC_API_URL=<url-do-backend-no-cloud-run>` |

A cada push na `main`, a loja é publicada sozinha.

### Opção 2 — pela linha de comando
```bash
npx wrangler login
NEXT_PUBLIC_API_URL=<url-do-backend> npm run deploy
```

Para testar o build do Cloudflare localmente antes de publicar: `npm run preview` (http://localhost:8787).

Depois do primeiro deploy, coloque a URL da loja em `FRONTEND_ORIGINS` e `STORE_URL` no backend.
