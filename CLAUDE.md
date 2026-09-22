# Contexto do Projeto — Loja de Jogos de Tabuleiro Infantis

## O que é
E-commerce para venda de jogos de tabuleiro voltados ao público infantil. A dona do negócio precisa conseguir operar a loja sozinha no dia a dia, com baixa manutenção técnica.

Referência de mercado: https://www.topgg.com.br

## Restrição de orçamento
Custo mensal de infraestrutura deve ficar **bem abaixo de R$110/mês** (na prática, próximo de R$0, usando tiers gratuitos). Nunca sugerir serviços pagos como padrão sem justificar.

## Stack (arquitetura de baixa manutenção — usar serviços gerenciados, nunca servidor próprio/VPS)
- **Frontend**: Next.js, hospedado no **Cloudflare Pages** (free tier permite uso comercial).
- **Backend**: **Java 17 + Spring Boot** (exigência do professor: backend em Java), na pasta `backend/`, hospedado no **Google Cloud Run** (free tier, escala a zero). Substitui os Cloudflare Workers do plano original, que não rodam Java. Expõe API REST (JSON) para a loja em Next.js e renderiza o **painel admin com Thymeleaf** (o admin em Next.js em `frontend/src/app/admin` fica só até a versão Thymeleaf ser confirmada).
- **Banco de dados**: **Supabase** (free tier: 500 MB), acessado pelo backend via JDBC/JPA (Session pooler), com esquema versionado por Flyway. Localmente o backend usa H2 em modo PostgreSQL.
- **Login do admin**: "Entrar com Google" direto no Spring Security (OAuth2), autorizando pelo e-mail cadastrado na tabela de admins.
- **Pagamentos**: **Mercado Pago Checkout Pro** — Pix, cartão, boleto. Nunca armazenar dados de cartão.
- **E-mail transacional**: **Resend** (free tier: 100 e-mails/dia).
- **Domínio**: registro.br (.com.br).

## Funcionalidades — Fase 1 (MVP)
- Catálogo de produtos organizado por categoria e faixa etária.
- Página de produto (fotos, descrição, idade recomendada, nº de jogadores, preço, habilidade estimulada).
- Carrinho de compras.
- Checkout com Mercado Pago (Pix e cartão).
- Comentários/avaliações de clientes em cada produto — **exigem aprovação do admin antes de ficarem visíveis** (moderação, público infantil).
- **Painel admin — controle total do site para quem tem acesso de admin:**
  - CRUD completo de produtos e categorias: adicionar, remover, editar — inclusive marcar/desmarcar estoque de itens esgotados.
  - Colocar/tirar um produto de promoção (liga o preço promocional, campo `promo` já existente no modelo de dados).
  - Editar o conteúdo da página **Sobre** (texto, imagens) direto pelo painel, sem depender de programador.
  - **Novidades**: ao adicionar um produto, o admin define por quanto tempo ele fica na aba Novidades (não é só um boolean fixo — precisa de um período configurável, ex.: data de expiração ou nº de dias) e consegue ver quanto tempo falta para cada item sair de lá.
  - **Gestão de admins**: quem já é admin pode adicionar (e remover) e-mails de outras pessoas como admin — lista de e-mails com esse papel, controlada pelo próprio painel, não hardcoded no código.
  - Visualização de pedidos.

## Funcionalidades — Fase 2 (depois do MVP)
- Cálculo automático de frete por CEP (ex: via Melhor Envio), com **toggle no admin para ligar/desligar** essa função.
- Dashboard admin com métricas: faturamento, número de pedidos, cliques/visualizações por produto.

## Progresso do frontend (visão do cliente)
Já implementado em `frontend/` (Next.js, App Router), com dados **mockados** em `frontend/src/app/data/products.js` — hoje sem Supabase conectado, tudo estático/local:
- **Home** (`/`), **Header** e **Footer** com a identidade visual definida abaixo.
- **Catálogo** (`/jogos`), com filtro por faixa etária e por "Cooperativos" via query params (`?idade=`, `?estilo=`).
- **Ficha de produto** (`/jogos/[slug]`): foto (placeholder), descrição, idade recomendada, nº de jogadores, preço (com preço promocional quando houver), habilidade estimulada, avaliações (com aviso de que passam por aprovação antes de aparecer) e produtos relacionados.
- **Novidades** (`/novidades`) e **Promoções** (`/promocoes`), filtrando o mesmo catálogo por `isNew` e `promo`.
- **Sobre** (`/sobre`) com história/missão/valores — texto provisório até a Donna enviar o conteúdo real.
- **Carrinho** (`/carrinho`), com quantidade e remoção funcionando no cliente (estado local em React; ainda sem persistência real).
- **Login** (`/login`) e **Cadastro** (`/cadastro`), com botão "Continuar com Google" — hoje só visual; ativar de verdade é só habilitar o provedor Google no Supabase Auth (gratuito, sem custo extra).
- **Painel admin** (`/admin/*`) — já construído e funcional, mas hoje ainda em cima dos mesmos dados mockados/locais (ver aviso abaixo), sem Supabase por trás:
  - `/admin/login`: acesso mockado por e-mail (sem senha real ainda), checado contra a lista de admins salva em `localStorage` (`asobi-admins`). E-mail seed: `dona@asobi.com.br`.
  - `/admin` (dashboard), `/admin/produtos` (lista com estoque editável inline, badge "Esgotado", status de promoção e de Novidades) + `/admin/produtos/novo` e `/admin/produtos/[slug]` (formulário de criar/editar, incluindo promoção e "dias em Novidades").
  - `/admin/sobre`: edita o texto da história, os 4 cards de valores e o bloco de missão da página `/sobre` — reflete no site na hora.
  - `/admin/avaliacoes`: fila de moderação (aprovar/rejeitar) das avaliações enviadas pelo formulário na ficha de produto — o formulário já manda a avaliação como pendente de verdade, e só aparece no produto depois de aprovada.
  - `/admin/admins`: adicionar/remover e-mails com acesso de admin.
  - O painel roda isolado do Header/Footer da loja (`SiteChrome.js` esconde o chrome público em rotas `/admin/*`) e tem sidebar própria (`AdminShell.js`).
  - **Catálogo agora é mutável**: `PRODUCTS` virou `INITIAL_PRODUCTS` (seed) em `data/products.js`; o catálogo "de verdade" vive em `ProductsContext` (client-side, persistido em `localStorage` sob `asobi-products`) — é isso que o admin edita e é isso que as páginas de cliente (`/jogos`, `/novidades`, `/promocoes`, ficha de produto, carrinho) leem via `useProducts()`. Produto ganhou campo `stock` (estoque) e trocou `isNew: boolean` por `newUntil: string | null` (data de expiração), com `isProductNew()`/`daysRemaining()` calculando o resto em `ProductsContext.js`.
  - Como isso ainda não é Supabase, essas páginas de catálogo tiveram que virar Client Components (antes eram Server Components com `generateStaticParams`/`generateMetadata` dinâmica por produto) — perderam SSG e metadata por-produto nessa fase; isso volta quando o catálogo migrar para o Supabase com Server Components lendo do banco.

**Ao integrar o Supabase** (próximo passo real, ainda não feito): a tabela de produtos deve seguir a mesma estrutura de campos usada em `INITIAL_PRODUCTS`/`ProductsContext` (agora incluindo `stock` e `newUntil`) para não exigir retrabalho nas telas já prontas; o conteúdo do Sobre e a lista de admins (hoje em `SiteContentContext`/`AdminContext` + `localStorage`) viram tabelas equivalentes; e a checagem de admin deve migrar do `localStorage` para o Supabase Auth + uma tabela `admins` (ou coluna `role` em `profiles`), validada no backend (Spring Boot, em `backend/`) — nunca só escondendo botão no frontend, que é o que o mock atual faz.

## Ideias de microsserviço (escolher 1 para implementar "de verdade" isolado)
1. Cálculo de frete (recomendado — desacopla de uma API externa instável).
2. Recomendação de produto por idade/habilidade.
3. Moderação de comentários (filtro de conteúdo antes da fila de aprovação).

## Convenções e decisões já tomadas
- Nenhum servidor próprio/VPS — só serviços gerenciados, para a cliente conseguir manter sozinha.
- Toda decisão de arquitetura deve priorizar "a dona consegue operar sem depender de programador no dia a dia".
- Paleta e tom visual: lúdico, colorido, pensado para pais navegando com/para crianças (não usar visual corporativo/sério).

## Convenção de nomes (padrão de mercado)
Tudo que precisa de nome (pastas, arquivos, pacotes, classes, variáveis, funções, tabelas, colunas, endpoints, branches, variáveis de ambiente) segue o padrão de mercado da tecnologia em questão. Na dúvida, usar o nome que um projeto open source conhecido usaria.
- **Idioma**: identificadores de código em **inglês** (`ProductService`, `newUntil`, tabela `products`, endpoint `/api/products`). Português fica só no que o usuário final vê: textos da interface, mensagens de erro exibidas e rotas públicas da loja (`/jogos`, `/novidades`, `/sobre`, boas para SEO). Comentários podem ser em português.
- **Pastas do repositório**: minúsculas e genéricas, sem sufixo de tecnologia: `frontend/`, `backend/` (não `Backend-java/`).
- **Java**: pacotes minúsculos por domínio (`br.com.asobi.catalog`), classes em PascalCase com sufixo do papel (`ProductController`, `ProductService`, `ProductRepository`, `ProductResponse`), métodos e variáveis em camelCase, constantes em UPPER_SNAKE_CASE.
- **Banco (PostgreSQL)**: tabelas e colunas em snake_case, tabelas no plural (`products`, `order_items`, `new_until`). Migrações Flyway no formato `V<n>__<descricao_em_snake_case>.sql`.
- **API REST**: recursos no plural, kebab-case, sem verbos (`GET /api/products/{slug}`, `POST /api/orders`); rotas de admin sob `/admin/**`.
- **JavaScript/React**: componentes em PascalCase (`ProductCard.js`), funções e variáveis em camelCase, CSS Modules como `Component.module.css`.
- **Variáveis de ambiente**: UPPER_SNAKE_CASE (`DB_URL`, `MERCADO_PAGO_ACCESS_TOKEN`).
- **Git**: branches `feat/...`, `fix/...`, `chore/...`; mensagens de commit no padrão Conventional Commits (`feat: ...`, `fix: ...`).

## Marca
Nome: **ASOBI** (do japonês 遊び, "brincadeira").

## Identidade visual
- Fundo: branco (#FFFFFF).
- Azul: #2E7CF6 (escuro para hover: #1E5FD1, tint para fundos suaves: #EAF2FF).
- Rosa: #FF5FA2 (escuro para hover: #E8408A, tint para fundos suaves: #FFEAF3).
- Texto/ink: #1E2A45 (azul-marinho escuro, não preto puro).
- Tipografia: **Fredoka** para títulos/logotipo (arredondada, lúdica), **Nunito** para texto corrido.
- Tom: "fun", colorido, lúdico — nunca visual corporativo/sério.
- Logotipo: wordmark "asobi" em minúsculas, sílaba "aso" em azul + "bi" em rosa.

## Estrutura de header (referência: topgg.com.br)
- Barra superior fina (frete/formas de pagamento).
- Header: logo, navegação principal (Jogos, Novidades, Promoções, Sobre), login/cadastro, botão de carrinho.
- Trilho de categorias por faixa etária abaixo do header (pills: Até 3 anos, 4 a 6 anos, 7 a 10 anos, +10 anos, Cooperativos).

## Estrutura de footer (referência: topgg.com.br)
- Coluna de marca: logo, descrição curta, ícones de redes sociais.
- Coluna "Institucional": Sobre a ASOBI, Política de Privacidade, Trocas/Devoluções e Garantia, Política de Frete, Fale Conosco.
- Coluna "Atendimento": telefone/WhatsApp, e-mail.
- Coluna "Formas de pagamento": badges Pix / Cartão / Boleto.
- Linha legal final: razão social + CNPJ.

## Informações pendentes que a cliente (Donna) precisa fornecer
- Nome definitivo da marca: ✅ ASOBI (decidido).
- Lista final de categorias/faixas etárias.
- Catálogo real de produtos (fotos, descrições, preços).
- Razão social e CNPJ (necessário para rodapé e para conta do Mercado Pago).
- Cidade/endereço da empresa.
- Canal de atendimento: telefone/WhatsApp e e-mail de suporte.
- Redes sociais da ASOBI (Instagram, TikTok etc.).
- Textos das políticas: Privacidade, Trocas/Devoluções/Garantia, Frete.
- Texto de "Sobre a ASOBI" (história/missão).
- **Atenção LGPD**: por envolver público infantil, a Política de Privacidade deve tratar explicitamente do cuidado com dados de criança (LGPD Art. 14). Nunca coletar dados da criança além de faixa etária/preferência — apenas dados do responsável adulto que compra.
