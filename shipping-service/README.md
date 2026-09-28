# ASOBI — shipping-service (microsserviço de frete)

Cota o frete de um pacote na transportadora (hoje: **Melhor Envio**) e devolve as opções de entrega.
É chamado **só pelo backend da loja** (`../backend`), nunca direto pelo navegador.

- Java 17 · Spring Boot 4.1 · sem banco de dados (stateless)
- Porta: **8081** localmente (`PORT` no Cloud Run)
- Hospedagem: Google Cloud Run, serviço próprio (`asobi-shipping-service`), escala a zero

## Responsabilidade (e o que NÃO é dele)

| Faz | Não faz (fica no backend) |
|---|---|
| Falar com a API do Melhor Envio (token, sandbox/produção, timeout) | Saber quais produtos existem ou quanto custam |
| Validar CEPs e volumes | Ligar/desligar o frete automático (toggle do painel) |
| Normalizar a resposta (preço, prazo, transportadora) e ordenar da mais barata para a mais cara | Frete grátis acima de um valor |
| Traduzir falhas da transportadora em mensagens amigáveis (RFC 9457) | Gravar o frete no pedido |

Assim, trocar de transportadora (ou somar outra) só mexe neste serviço.

## API

Todas as rotas `/api/**` exigem o cabeçalho `X-Api-Key` (igual a `SHIPPING_SERVICE_API_KEY`).

| Método e rota | Descrição |
|---|---|
| `POST /api/quotes` | Cota o frete. Corpo: `originPostalCode`, `destinationPostalCode`, `parcels[]` (`id`, `quantity`, `unitValue`, `weightKg`, `widthCm`, `heightCm`, `lengthCm`). Resposta: `[{id, name, company, price, deliveryDays}]` |
| `GET /api/provider` | `{"name": "melhor-envio", "configured": true}`: diz se há token para cotar (o painel usa isso para liberar o toggle) |
| `GET /actuator/health` | Health check (aberto, sem chave) |

Erros (`application/problem+json`): `400` dados inválidos · `401` chave errada · `502` a transportadora falhou · `503` sem token configurado.

## Rodar localmente

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS/Linux
```

No backend, defina `SHIPPING_SERVICE_URL=http://localhost:8081` para ele usar este serviço.
Ou suba os dois juntos com `docker compose up --build` na raiz do repositório.

Testes: `.\mvnw.cmd test`

## Variáveis de ambiente

| Variável | Exemplo |
|---|---|
| `SHIPPING_SERVICE_API_KEY` | chave longa aleatória, a mesma configurada no backend. Vazia = sem conferência (só local) |
| `MELHOR_ENVIO_TOKEN` | token da conta Melhor Envio (permissão `shipping-calculate`). Vazio = cotação indisponível |
| `MELHOR_ENVIO_SANDBOX` | `true` (padrão, ambiente de testes) / `false` em produção |
| `MELHOR_ENVIO_CONTACT_EMAIL` | e-mail técnico enviado no User-Agent (exigência do Melhor Envio) |

## Deploy no Cloud Run

De dentro de `shipping-service/`:

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

Depois, no backend: `SHIPPING_SERVICE_URL=<url-deste-serviço>` e o mesmo segredo `asobi-shipping-api-key`
em `SHIPPING_SERVICE_API_KEY`. (`--allow-unauthenticated` libera o acesso de rede; quem protege a API é a `X-Api-Key`.)
