-- Conta do cliente (login com Google pelo Supabase Auth). Preenchido quando o pedido
-- é feito logado; pedidos sem cadastro continuam com NULL.
ALTER TABLE orders ADD COLUMN customer_user_id UUID;

CREATE INDEX idx_orders_customer_user ON orders (customer_user_id);
CREATE INDEX idx_orders_customer_email ON orders (customer_email);
