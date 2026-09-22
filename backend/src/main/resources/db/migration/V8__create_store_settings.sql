-- Configurações da loja editáveis no painel (/admin/configuracoes). Linha única.
CREATE TABLE store_settings (
    id                      BIGINT PRIMARY KEY CHECK (id = 1),
    -- Liga/desliga o cálculo automático de frete (Melhor Envio). Desligado = "a combinar".
    shipping_enabled        BOOLEAN        NOT NULL DEFAULT FALSE,
    origin_postal_code      VARCHAR(9),
    -- Compras a partir deste valor ganham a opção de frete mais barata de graça (NULL = sem frete grátis).
    free_shipping_threshold NUMERIC(10, 2),
    -- Caixa padrão de um jogo, usada na cotação.
    package_weight_kg       NUMERIC(6, 3)  NOT NULL DEFAULT 1.000,
    package_width_cm        INT            NOT NULL DEFAULT 30,
    package_height_cm       INT            NOT NULL DEFAULT 8,
    package_length_cm       INT            NOT NULL DEFAULT 30,
    updated_by_email        VARCHAR(254),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO store_settings (id, free_shipping_threshold) VALUES (1, 150.00);

-- Serviço de entrega escolhido no checkout (ex.: "Correios PAC · 5 dias úteis").
ALTER TABLE orders ADD COLUMN shipping_service VARCHAR(120);
