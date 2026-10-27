# ASOBI — loja (Next.js)

É o site que o cliente vê: lista de jogos, página de cada jogo, carrinho, finalização da compra,
acompanhamento do pedido e "Meus pedidos".

Esta parte **não guarda dados**: tudo (jogos, preços, textos) vem do backend ([`backend`](backend.md)).
O painel do lojista também fica no backend; aqui, `/admin` só manda a pessoa para lá.

- Feito com Next.js 16, React 19 e CSS Modules (um arquivo `.module.css` por componente)
- Fica hospedado na Cloudflare Workers, usando o [OpenNext](https://opennext.js.org/cloudflare)

## Como rodar no computador

Primeiro ligue o backend (porta 8080). Depois:

```bash
cp .env.example .env.local   # mude se o backend não estiver em localhost:8080
npm install
npm run dev                  # abre em http://localhost:3000
```

- `npm run lint` procura erros no código.
- `npm run build` gera a versão final do site.

## Configurações (variáveis de ambiente)

| Variável | Para que serve |
|---|---|
| `NEXT_PUBLIC_API_URL` | Endereço do backend. Se não preencher, usa `http://localhost:8080` |
| `NEXT_PUBLIC_SUPABASE_URL` | Login de cliente: `https://<id-do-projeto>.supabase.co` |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Login de cliente: a chave **pública** do Supabase (`anon` / `publishable`). Nunca a secreta |

Se as duas do Supabase ficarem vazias, a loja funciona igual, só some o botão "Entrar".
Dá para comprar sem cadastro do mesmo jeito.

## De onde cada página tira os dados

- **Rodapé, barra do topo e políticas**: telefone, redes sociais, razão social/CNPJ, valor do frete grátis
  e os textos das políticas e do "Fale conosco" são preenchidos pelo lojista no painel.
  Campo vazio não aparece no site.
- **Páginas do catálogo** (`src/app/lib/catalog.js`): a home, `/jogos`, `/novidades`, `/promocoes`, `/sobre`
  e a página de cada jogo são montadas no servidor. O site guarda as respostas do backend por
  **1 minuto**, então uma mudança feita no painel leva até 1 minuto para aparecer. Cada jogo tem título
  e descrição próprios, o que ajuda no Google.
- **Carrinho e checkout** (`src/app/lib/useProducts.js`): rodam no navegador e consultam preço e estoque
  na hora, para não vender nada com preço velho.
- Se o backend estiver fora do ar, a página mostra um aviso em vez de dar erro.

## Login de cliente (com Google, pelo Supabase)

O cliente clica em `/login` → "Continuar com Google" → volta para `/conta` ("Meus pedidos").
Não tem senha nem tela de cadastro: a conta é criada no primeiro login. Quem está logado tem o pedido
ligado à conta, e pedidos antigos feitos sem cadastro com o mesmo e-mail também aparecem.

Para ligar o login (só uma vez, no site do Supabase):

1. **Authentication → Sign In / Providers → Google**: ative e cole o *Client ID* e o *Client Secret*
   criados no [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   (tipo *Aplicativo da Web*, com o endereço de retorno que o Supabase mostra nessa tela:
   `https://<id-do-projeto>.supabase.co/auth/v1/callback`).
2. **Authentication → URL Configuration**: em *Site URL* coloque o endereço da loja e, em *Redirect URLs*,
   `http://localhost:3000/**` e `https://<dominio-da-loja>/**`.
3. Preencha `NEXT_PUBLIC_SUPABASE_URL` e `NEXT_PUBLIC_SUPABASE_ANON_KEY` aqui, e `SUPABASE_URL` e
   `SUPABASE_SERVICE_KEY` no backend (é o backend que confere se o login é válido).

## Colocar no ar (Cloudflare)

Como as páginas são montadas no servidor, a loja vai para o **Workers** (gratuito), e não para o
Pages, que só serve arquivos prontos. A configuração está em `frontend/wrangler.jsonc` e `frontend/open-next.config.ts`.

```bash
npx wrangler login
npx wrangler r2 bucket create asobi-frontend-cache   # só na primeira vez
npm run deploy
```

- As variáveis `NEXT_PUBLIC_*` são gravadas no site na hora do build. Por isso, coloque-as em
  `.env.production` **antes** de rodar `npm run deploy`.
- As páginas prontas ficam guardadas num espaço da Cloudflare chamado **R2** (gratuito até 10 GB e
  1 milhão de gravações por mês; a Cloudflare pede um cartão para ativar, mas não cobra dentro desse limite).
- `npm run preview` roda no computador a mesma versão que vai para a Cloudflare. No Windows, se o build
  der problema, o OpenNext recomenda usar o WSL.
