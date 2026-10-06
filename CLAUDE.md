# Contexto do Projeto — Loja de Jogos de Tabuleiro Infantis

## O que é
E-commerce para venda de jogos de tabuleiro voltados ao público infantil. A Priscila, responsável pelo negócio, precisa conseguir operar a loja sozinha no dia a dia, com baixa manutenção técnica.

Referência de mercado: https://www.topgg.com.br

## Restrição de orçamento
Custo mensal de infraestrutura deve ficar **bem abaixo de R$110/mês** (na prática, próximo de R$0, usando tiers gratuitos). Nunca sugerir serviços pagos como padrão sem justificar.

## Stack (arquitetura de baixa manutenção — usar serviços gerenciados, nunca servidor próprio/VPS)
- **Frontend**: Next.js, hospedado na **Cloudflare** (Workers, pelo OpenNext; free tier permite uso comercial). Precisa de servidor porque o catálogo é renderizado no servidor; o cache das páginas fica num bucket R2.
- **Backend**: **Java 17 + Spring Boot** (exigência do professor: backend em Java), na pasta `backend/`, hospedado no **Google Cloud Run** (free tier, escala a zero). Substitui os Cloudflare Workers do plano original, que não rodam Java. Expõe API REST (JSON) para a loja em Next.js e renderiza o **painel admin com Thymeleaf**.
- **Banco de dados**: **Supabase** (free tier: 500 MB), acessado pelo backend via JDBC/JPA (Session pooler), com esquema versionado por Flyway. Localmente o backend usa H2 em modo PostgreSQL.
- **Login do cliente**: "Continuar com Google" pelo **Supabase Auth** (gratuito), no navegador; o backend confere o token no próprio Supabase. Sem senha e sem tela de cadastro. Comprar sem cadastro continua possível.
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

## Progresso do frontend (`frontend/`, Next.js App Router)
Todos os dados vêm da API do backend (`NEXT_PUBLIC_API_URL`, padrão `http://localhost:8080`). O admin em Next.js, os dados mockados e o modo `localStorage` foram **removidos**; `/admin` na loja só redireciona para o painel do Spring. Detalhes de execução, variáveis e deploy no `frontend/README.md`.
- **Catálogo em Server Components** (`src/app/lib/catalog.js`, cache de 1 minuto): home (destaques e faixas etárias reais), `/jogos` (filtros `?idade=` e `?estilo=cooperativos` resolvidos no servidor), `/novidades`, `/promocoes`, `/sobre` e a ficha `/jogos/[slug]` (com `generateStaticParams` e `generateMetadata`: título, descrição e imagem por produto). Se o backend não responde, as listas mostram um aviso em vez de quebrar.
- **Componentes de cliente** só onde há interação: Header (carrinho, menu, link da conta), galeria de fotos, "adicionar ao carrinho", formulário de avaliação (`ReviewForm`), carrinho, checkout e página do pedido. Carrinho e checkout buscam preço/estoque na hora (`src/app/lib/useProducts.js`).
- **Conta do cliente**: `/login` (Google), `/conta` ("Meus pedidos") e checkout enviando o token quando logado (`AuthContext`, `src/app/lib/supabase.js`). Só aparece com `NEXT_PUBLIC_SUPABASE_URL` e `NEXT_PUBLIC_SUPABASE_ANON_KEY` definidos.
- **Deploy**: `npm run deploy` (OpenNext → Cloudflare Workers; `wrangler.jsonc`, `open-next.config.ts`).

## Progresso do backend (`backend/`, branch `feat/backend-java`)
Spring Boot 4.1 + Java 17, organizado por domínio (`catalog`, `review`, `order`, `payment`, `shipping`, `content`, `settings`, `admin`, `storage`, `notification`, `account`), cada um com controller/service/repository/model/dto. Esquema do banco em migrações Flyway (`src/main/resources/db/migration`). Detalhes de execução, variáveis de ambiente e deploy no `backend/README.md`.
- **API REST para a loja** (`/api/**`, pública, CORS para o Next.js, erros em RFC 9457): catálogo e faixas etárias, conteúdo do Sobre, envio de avaliações (sempre entram pendentes), pedidos (preço e estoque calculados no servidor), acompanhamento do pedido, cotação de frete, webhook do Mercado Pago.
- **Painel admin em Thymeleaf** (`/admin/**`): login com Google + e-mail na tabela `admin_users` (conferido no banco a cada requisição), sessão no banco (Spring Session JDBC). Telas: dashboard, pedidos, produtos, faixas etárias, avaliações, página Sobre, configurações (liga/desliga frete) e admins.
- **Pagamento**: Mercado Pago Checkout Pro (webhook com assinatura conferida; o pagamento é sempre reconsultado na API). Sem token do Mercado Pago, roda em modo manual (a Priscila marca como pago no painel).
- **Estoque** baixa só quando o pagamento é aprovado, com lock na linha do produto.
- **Conta do cliente** (módulo `account`): `GET /api/account/orders` exige `Authorization: Bearer <token do Supabase>`; `POST /api/orders` aceita o token como opcional e grava `orders.customer_user_id`. "Meus pedidos" traz os pedidos da conta e os feitos sem cadastro com o mesmo e-mail confirmado. Sem `SUPABASE_URL`/`SUPABASE_SERVICE_KEY`, o login de cliente fica desligado.
- **Fotos de produto** (módulo `storage` + tabela `product_images`): até 6 por produto, enviadas no formulário do painel (o navegador reduz a foto antes de enviar), com capa e exclusão. Ficam no **Supabase Storage** (bucket público `product-images`, criado sozinho) quando `SUPABASE_URL` e `SUPABASE_SERVICE_KEY` estão definidos; sem eles, numa pasta local (`data/uploads`, só para desenvolvimento). A API devolve `images` (URLs, capa primeiro); sem foto, a loja mostra o emoji de `icon`.
- **E-mails de pedido** (módulo `notification`, Resend): pedido recebido, pagamento confirmado e pedido enviado para o cliente, mais um aviso de pedido pago para os admins. O `order` só publica `OrderEvent`; o envio acontece depois do commit e nunca derruba o pedido. Sem `RESEND_API_KEY`, os e-mails só aparecem no log.
- Pendências conhecidas: dashboard de métricas (Fase 2); busca do Header e newsletter da home ainda são só visuais.

## Microsserviços
- **Implementado: `shipping-service/`** (Spring Boot, porta 8081 local, Cloud Run próprio, sem banco). Só ele fala com o Melhor Envio (guarda o `MELHOR_ENVIO_TOKEN`); o backend chama `POST /api/quotes` via `ShippingServiceClient` com `X-Api-Key` e mantém as regras comerciais (toggle, frete grátis). Detalhes em `shipping-service/README.md`.
- Portas locais: frontend 3000, backend 8080, shipping-service 8081 (`docker-compose.yml` na raiz sobe os dois Java).
- Análise completa (necessidades → requisitos → funcionalidades → processos → domínios → serviços, limites, dependências, fluxos, portas) em `docs/arquitetura.md` — manter atualizado ao mudar a arquitetura.
- Candidatos futuros: moderação de comentários (filtro antes da fila) e recomendação por idade/habilidade.

## Convenções e decisões já tomadas
- Nenhum servidor próprio/VPS — só serviços gerenciados, para a cliente conseguir manter sozinha.
- Toda decisão de arquitetura deve priorizar "a Priscila consegue operar sem depender de programador no dia a dia".
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

## Informações pendentes que a cliente precisa fornecer
- Nome definitivo da marca: ✅ ASOBI .
- Lista final de categorias/faixas etárias.
- Catálogo real de produtos (fotos, descrições, preços).
- Razão social e CNPJ (necessário para rodapé e para conta do Mercado Pago).
- Cidade/endereço da empresa.
- Canal de atendimento: telefone/WhatsApp e e-mail de suporte.
- Redes sociais da ASOBI (Instagram, TikTok etc.).
- Textos das políticas: Privacidade, Trocas/Devoluções/Garantia, Frete.
- Texto de "Sobre a ASOBI" (história/missão).
- **Atenção LGPD**: por envolver público infantil, a Política de Privacidade deve tratar explicitamente do cuidado com dados de criança (LGPD Art. 14). Nunca coletar dados da criança além de faixa etária/preferência — apenas dados do responsável adulto que compra.
