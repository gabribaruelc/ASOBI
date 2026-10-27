# ASOBI — two-factor-service (serviço de código por e-mail)

É a "segunda etapa" do login do painel. Depois que o admin entra com o Google, este serviço manda um
código de 6 números para o e-mail dele e depois confere se o código digitado está certo.
Assim, mesmo que alguém descubra a senha do Google, ainda precisa ter acesso ao e-mail.

Só o backend da loja ([`backend`](backend.md)) conversa com ele. O navegador nunca chama este serviço direto.

- Java 17, Spring Boot 4.1, JPA (acesso ao banco) e Flyway (cria as tabelas)
- Banco: um esquema só dele no Supabase, chamado `two_factor` (no computador usa o H2, um banco em arquivo)
- Roda na porta **8082** no computador
- No ar, fica no Google Cloud Run (serviço `asobi-two-factor-service`), que desliga sozinho quando ninguém usa

## O que ele faz e o que não faz

| Faz | Não faz (isso é do backend) |
|---|---|
| Sortear o código (6 números, de forma segura) e mandar por e-mail pelo Resend | Decidir quando e para quem pedir o código |
| Guardar no banco só uma "impressão digital" do código (HMAC), nunca o código | Mostrar a tela onde a pessoa digita o código |
| Conferir o código: vale por 10 minutos, só pode ser usado uma vez e aceita no máximo 5 erros | Lembrar que a pessoa já passou pela conferência |
| Limitar a 5 códigos por e-mail a cada 15 minutos | Saber qualquer coisa de produtos, pedidos ou admins |
| Cancelar o código anterior quando a pessoa pede outro | |
| Se o mesmo pedido chegar repetido em até 30 segundos, devolver o mesmo código sem mandar outro e-mail | |

### Como funciona, passo a passo

1. O backend chama `POST /api/challenges` com o e-mail e o motivo (`purpose`, ex.: `admin-login`)
   e guarda o `id` que recebe de volta.
2. A pessoa recebe o código no e-mail e digita na tela do backend.
3. O backend manda o código para `POST /api/challenges/{id}/verifications`.
4. Só passa quem receber `verified: true`. O backend ainda confere se o `email` e o `purpose` da resposta
   são os que ele esperava.

### Por que o código não fica salvo

O código nunca aparece em nenhuma resposta: só existe no e-mail. No banco fica um HMAC-SHA256 dele,
que é uma espécie de "impressão digital" feita com uma chave secreta que só este serviço conhece
(`TWO_FACTOR_CODE_SECRET`). Um hash comum (sem chave) não serviria: como só existem um milhão de
códigos possíveis, quem visse o banco conseguiria testar todos rapidinho.

## Rotas

Toda rota que começa com `/api/` exige o cabeçalho `X-Api-Key` com a mesma senha de `TWO_FACTOR_SERVICE_API_KEY`.

| Rota | O que faz |
|---|---|
| `POST /api/challenges` | Recebe `{"email": "...", "purpose": "admin-login"}`, manda o código e responde `201` com `{id, email, purpose, expiresAt}` |
| `POST /api/challenges/{id}/verifications` | Recebe `{"code": "123456"}`. Se o código estiver certo, responde `200` com `{id, email, purpose, verified: true}` |
| `GET /actuator/health` | Diz se o serviço está no ar. Não precisa de senha |

O `purpose` aceita letras minúsculas, números e hífen (até 40 caracteres).

Quando dá erro, a resposta vem em JSON e o campo `detail` já tem uma mensagem pronta para mostrar na tela:

| Código | Quando acontece |
|---|---|
| `400` | Dados errados (o campo `errors` diz o problema de cada campo) |
| `401` | Senha (`X-Api-Key`) errada |
| `404` | Pedido de código não encontrado |
| `410` | Código vencido, já usado ou bloqueado por muitos erros: precisa pedir outro |
| `422` | Código errado; `attemptsLeft` diz quantas tentativas ainda restam |
| `429` | O e-mail pediu códigos demais em pouco tempo |
| `503` | Não deu para mandar o e-mail; nenhum código fica valendo |

## Como rodar no computador

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

Ele sobe com o banco H2 na pasta `data/two-factor`. Sem `RESEND_API_KEY`, nenhum e-mail é enviado:
o e-mail, **com o código**, aparece no log (a saída do terminal). É assim que se testa no computador.
Outra opção é subir tudo junto com `docker compose up --build` na pasta principal do projeto.

```bash
curl -X POST localhost:8082/api/challenges -H "Content-Type: application/json" \
  -d '{"email": "admin@asobi.com.br", "purpose": "admin-login"}'
# copie o código que apareceu no log e o id da resposta
curl -X POST localhost:8082/api/challenges/<id>/verifications -H "Content-Type: application/json" \
  -d '{"code": "123456"}'
```

Testes: `.\mvnw.cmd test`

## Configurações (variáveis de ambiente)

| Variável | O que colocar |
|---|---|
| `TWO_FACTOR_SERVICE_API_KEY` | uma senha longa e aleatória, igual à do backend. Se ficar vazia, não confere a senha (só use assim no computador) |
| `TWO_FACTOR_CODE_SECRET` | uma chave longa e aleatória usada no HMAC. **Obrigatória no ar**: se ficar vazia, o serviço cria uma nova toda vez que liga, e como o Cloud Run desliga quando ninguém usa, os códigos enviados antes deixariam de funcionar |
| `RESEND_API_KEY` | chave do Resend. Vazia = o e-mail só aparece no log, com o código (nunca deixe assim no ar) |
| `EMAIL_FROM` | `ASOBI <acesso@asobi.com.br>`; o domínio precisa estar verificado no Resend |
| `DB_URL` | `jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require` (com o perfil `supabase`) |
| `DB_USER` / `DB_PASSWORD` | usuário e senha deste serviço no Supabase (veja abaixo) |
| `DB_SCHEMA` | opcional; se não preencher, usa `two_factor` |

O tempo de validade do código, o número de tentativas e o limite de pedidos ficam em `two-factor.*` no `application.yml`.

### Criando o usuário e o esquema no Supabase

O serviço usa o mesmo projeto do Supabase que a loja (para continuar no plano grátis), mas com um
esquema e um usuário separados, sem acesso às tabelas do backend. No SQL Editor do Supabase, uma vez só:

```sql
CREATE SCHEMA two_factor;
CREATE ROLE two_factor_service LOGIN PASSWORD '<senha-forte>';
GRANT ALL ON SCHEMA two_factor TO two_factor_service;
ALTER ROLE two_factor_service SET search_path = two_factor;
```

Pelo Session pooler do Supabase, o `DB_USER` fica `two_factor_service.<id-do-projeto>`.
A tabela `challenges` é criada sozinha pelo Flyway na primeira vez que o serviço sobe.
Códigos vencidos há mais de um dia são apagados pelo próprio serviço sempre que alguém pede um código novo.

## Colocar no ar (Cloud Run)

Dentro da pasta `two-factor-service/`:

```bash
gcloud run deploy asobi-two-factor-service \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 1 \
  --set-env-vars "EMAIL_FROM=ASOBI <acesso@asobi.com.br>" \
  --set-secrets TWO_FACTOR_SERVICE_API_KEY=asobi-two-factor-api-key:latest,TWO_FACTOR_CODE_SECRET=asobi-two-factor-code-secret:latest,RESEND_API_KEY=asobi-resend-api-key:latest,DB_URL=asobi-db-url:latest,DB_USER=asobi-two-factor-db-user:latest,DB_PASSWORD=asobi-two-factor-db-password:latest
```

O `--allow-unauthenticated` só deixa o serviço acessível pela internet; quem barra estranhos é a senha do `X-Api-Key`.

Cada código gasta um e-mail do plano grátis do Resend (100 por dia, contando junto com os e-mails de pedido).
