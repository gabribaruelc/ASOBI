# ASOBI — shipping-service (serviço de frete)

Calcula quanto custa e quanto tempo demora para entregar um pacote. Hoje ele pergunta isso ao
**Melhor Envio** e devolve as opções (ex.: PAC, SEDEX) com preço e prazo.

Só o backend da loja ([`backend`](backend.md)) conversa com ele. O navegador do cliente nunca chama este serviço direto.

- Java 17 e Spring Boot 4.1
- Não tem banco de dados: recebe o pedido de cotação, pergunta ao Melhor Envio e responde
- Roda na porta **8081** no computador
- No ar, fica no Google Cloud Run (serviço `asobi-shipping-service`), que desliga sozinho quando ninguém usa

## O que ele faz e o que não faz

| Faz | Não faz (isso é do backend) |
|---|---|
| Conversar com o Melhor Envio (usando o token, em teste ou produção, com tempo limite) | Saber quais jogos existem ou quanto custam |
| Conferir se os CEPs e as medidas dos pacotes estão certos | Ligar ou desligar o frete automático (botão do painel) |
| Deixar a resposta num formato simples (preço, prazo, transportadora), da mais barata para a mais cara | Dar frete grátis acima de um valor |
| Transformar erros do Melhor Envio em mensagens fáceis de entender | Gravar o frete no pedido |

A vantagem de separar: se um dia a loja trocar de transportadora, só este serviço muda.

## Rotas

Toda rota que começa com `/api/` exige o cabeçalho `X-Api-Key` com a mesma senha de `SHIPPING_SERVICE_API_KEY`.

| Rota | O que faz |
|---|---|
| `POST /api/quotes` | Calcula o frete. Recebe `originPostalCode` (CEP de origem), `destinationPostalCode` (CEP de destino) e `parcels[]` (os pacotes: `id`, `quantity`, `unitValue`, `weightKg`, `widthCm`, `heightCm`, `lengthCm`). Devolve `[{id, name, company, price, deliveryDays}]` |
| `GET /api/provider` | Responde `{"name": "melhor-envio", "configured": true}`, dizendo se o token foi configurado (o painel usa isso para liberar o botão do frete) |
| `GET /actuator/health` | Diz se o serviço está no ar. Não precisa de senha |

Quando dá erro, a resposta vem em JSON com a explicação:
`400` dados errados · `401` senha errada · `502` o Melhor Envio falhou · `503` falta o token.

## Como rodar no computador

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

No backend, coloque `SHIPPING_SERVICE_URL=http://localhost:8081` para ele usar este serviço.
Outra opção é subir tudo junto com `docker compose up --build` na pasta principal do projeto.

Testes: `.\mvnw.cmd test`

## Configurações (variáveis de ambiente)

No computador, ficam no arquivo `shipping-service/.env` (tem um modelo em `.env.example`).

| Variável | O que colocar |
|---|---|
| `SHIPPING_SERVICE_API_KEY` | uma senha longa e aleatória, igual à do backend. Se ficar vazia, não confere a senha (só use assim no computador) |
| `MELHOR_ENVIO_TOKEN` | token da conta do Melhor Envio (com a permissão `shipping-calculate`). Vazio = não calcula frete |
| `MELHOR_ENVIO_SANDBOX` | `true` usa o ambiente de testes do Melhor Envio (padrão); `false` usa o de verdade |
| `MELHOR_ENVIO_CONTACT_EMAIL` | um e-mail de contato técnico, que o Melhor Envio exige em toda chamada |

## Colocar no ar (Cloud Run)

Dentro da pasta `shipping-service/`:

```bash
gcloud run deploy asobi-shipping-service \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --min-instances 0 --max-instances 1 \
  --set-env-vars MELHOR_ENVIO_SANDBOX=false,MELHOR_ENVIO_CONTACT_EMAIL=<email-tecnico> \
  --set-secrets SHIPPING_SERVICE_API_KEY=asobi-shipping-api-key:latest,MELHOR_ENVIO_TOKEN=asobi-melhor-envio-token:latest
```

Depois, no backend, coloque `SHIPPING_SERVICE_URL=<endereço-deste-serviço>` e a mesma senha
(`asobi-shipping-api-key`) em `SHIPPING_SERVICE_API_KEY`.

O `--allow-unauthenticated` só deixa o serviço acessível pela internet; quem barra estranhos é a senha do `X-Api-Key`.
