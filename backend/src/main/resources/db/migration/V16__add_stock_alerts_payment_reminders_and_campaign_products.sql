-- Estoque baixo: com este número de unidades ou menos, os admins recebem um aviso, a loja mostra
-- "últimas unidades" e quem pediu esse aviso recebe o e-mail. 0 = desligado.
ALTER TABLE store_settings ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 3;

-- Lembrete único para quem fez o pedido e não chegou a pagar.
ALTER TABLE orders ADD COLUMN payment_reminder_sent_at TIMESTAMP WITH TIME ZONE;

-- Jogos em destaque numa newsletter (cartões com foto, preço e link). O preço é lido na hora do
-- envio; o jogo que sai do catálogo some da newsletter.
CREATE TABLE newsletter_campaign_products (
    campaign_id BIGINT NOT NULL REFERENCES newsletter_campaigns (id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    PRIMARY KEY (campaign_id, product_id)
);

CREATE INDEX idx_newsletter_campaign_products_product ON newsletter_campaign_products (product_id);
