-- Dados de contato e do rodapé da loja, editáveis no painel (/admin/contato). Linha única.
-- Campo vazio (NULL) simplesmente não aparece na loja.
CREATE TABLE site_info (
    id               BIGINT PRIMARY KEY CHECK (id = 1),
    contact_email    VARCHAR(254),
    -- Telefone/WhatsApp de atendimento, como deve aparecer: (11) 91234-5678
    contact_phone    VARCHAR(30),
    business_hours   VARCHAR(100),
    instagram_url    VARCHAR(200),
    facebook_url     VARCHAR(200),
    tiktok_url       VARCHAR(200),
    legal_name       VARCHAR(150),
    cnpj             VARCHAR(18),
    address          VARCHAR(200),
    updated_by_email VARCHAR(254),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO site_info (id, contact_email, business_hours)
VALUES (1, 'contato@asobi.com.br', 'Seg. a sex., 9h às 18h');

-- Textos das políticas, editáveis no painel (/admin/politicas). O slug é fixo: cada
-- um corresponde a uma página da loja.
CREATE TABLE policy_pages (
    slug             VARCHAR(40) PRIMARY KEY,
    position         INT            NOT NULL,
    title            VARCHAR(100)   NOT NULL,
    -- Texto simples: linha em branco separa parágrafos; linha começando com "# " vira subtítulo.
    body             VARCHAR(20000) NOT NULL,
    updated_by_email VARCHAR(254),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rascunhos provisórios, até a Priscila enviar os textos definitivos.
INSERT INTO policy_pages (slug, position, title, body) VALUES
('privacy', 1, 'Política de Privacidade',
'A ASOBI vende jogos de tabuleiro infantis, mas quem compra é sempre um adulto. Esta política explica quais dados coletamos e para quê.

# Quais dados coletamos
Só os dados do responsável adulto que faz a compra: nome, e-mail, telefone e endereço de entrega. Quem entra com a conta Google também tem o e-mail e o nome da conta associados aos pedidos.

# Dados de crianças
Não coletamos nome, idade, foto ou qualquer outro dado de crianças. A única informação relacionada a elas é a faixa etária do jogo escolhido, que não identifica ninguém. Seguimos o artigo 14 da Lei Geral de Proteção de Dados (LGPD): qualquer tratamento de dados de crianças depende do consentimento específico de um dos pais ou do responsável legal.

# Para que usamos os dados
Para processar o pedido, entregar a compra, enviar os e-mails sobre o andamento do pedido e atender você. Não vendemos nem compartilhamos seus dados para publicidade.

# Com quem compartilhamos
Com o Mercado Pago, que processa o pagamento (a ASOBI não tem acesso aos dados do seu cartão), e com a transportadora que faz a entrega.

# Avaliações de produtos
As avaliações enviadas passam por aprovação antes de aparecer na loja. Pedimos que não incluam dados pessoais de crianças.

# Seus direitos
Você pode pedir para consultar, corrigir ou excluir seus dados a qualquer momento pelos canais da página Fale Conosco.'),
('returns', 2, 'Trocas, Devoluções e Garantia',
'# Arrependimento da compra
Você pode desistir da compra em até 7 dias corridos depois de receber o produto, como prevê o Código de Defesa do Consumidor. O jogo deve voltar sem uso, na embalagem original e com todas as peças. Devolvemos o valor pago, incluindo o frete.

# Produto com defeito
Se o jogo chegar com defeito ou com peça faltando, fale com a gente em até 90 dias depois do recebimento. Trocamos o produto ou enviamos a peça, sem custo para você.

# Como pedir
Entre em contato pelos canais da página Fale Conosco, informando o número do pedido e, se for defeito, uma foto do problema. Respondemos com as instruções de envio.

# Reembolso
O reembolso é feito pelo mesmo meio de pagamento da compra, depois que recebemos e conferimos o produto.'),
('shipping', 3, 'Política de Frete',
'# Como o frete é calculado
O valor e o prazo aparecem no checkout, a partir do CEP de entrega. Quando o cálculo automático não está disponível, combinamos o frete com você antes do envio.

# Prazo de envio
O pedido é postado depois da confirmação do pagamento. O prazo de entrega informado no checkout começa a contar a partir da postagem.

# Acompanhamento
Quando o pedido é enviado, você recebe um e-mail com o código de rastreio. Também dá para acompanhar pela página do pedido.

# Problemas na entrega
Se o pedido atrasar ou voltar para a ASOBI, fale com a gente pelos canais da página Fale Conosco.');
