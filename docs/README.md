# Documentação da ASOBI

| Documento | Assunto |
|---|---|
| [arquitetura.md](arquitetura.md) | Visão geral: problema, requisitos, divisão em serviços, dados e fluxos |
| [frontend.md](frontend.md) | A loja (Next.js): como rodar, configurações e deploy na Cloudflare |
| [backend.md](backend.md) | O backend e o painel do lojista: pagamento, frete, fotos, e-mails e deploy |
| [shipping-service.md](shipping-service.md) | Serviço de frete (Melhor Envio) |
| [admin-auth-service.md](admin-auth-service.md) | Serviço de login dos admins (Google + lista de admins) |
| [two-factor-service.md](two-factor-service.md) | Serviço de código por e-mail do login do painel |
| [notification-service.md](notification-service.md) | Serviço de avisos de produto ("avise-me") |
| [status-service.md](status-service.md) | Serviço que avisa os admins por e-mail quando o site sai do ar |

Para subir os seis serviços Java juntos: `docker compose up --build` na pasta principal do projeto.
