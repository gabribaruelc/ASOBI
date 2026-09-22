-- Catálogo inicial (o mesmo de frontend/src/app/data/products.js), até a Donna
-- cadastrar os produtos reais pelo painel admin.
INSERT INTO categories (slug, name, position) VALUES
    ('ate-3',   'Até 3 anos',  1),
    ('4-6',     '4 a 6 anos',  2),
    ('7-10',    '7 a 10 anos', 3),
    ('mais-10', '+10 anos',    4);

INSERT INTO products (slug, name, icon, description, skill, category_id, age_label, players, price, original_price, stock, cooperative, new_until) VALUES
    ('corrida-dos-sapos', 'Corrida dos Sapos', '🐸',
     'Um jogo de tabuleiro cheio de saltos e decisões rápidas! As crianças competem para levar seus sapinhos até a lagoa, treinando o raciocínio lógico a cada jogada.',
     'Raciocínio lógico', (SELECT id FROM categories WHERE slug = '4-6'), '6+ anos', '2 a 4 jogadores', 89.90, NULL, 18, FALSE, NULL),
    ('missao-no-castelo', 'Missão no Castelo', '🏰',
     'Um jogo cooperativo em que todos jogam juntos contra o tabuleiro para resgatar o reino antes que o tempo acabe, estimulando o trabalho em equipe.',
     'Trabalho em equipe', (SELECT id FROM categories WHERE slug = '7-10'), '7+ anos', '2 a 6 jogadores', 119.90, NULL, 9, TRUE, TIMESTAMP WITH TIME ZONE '2026-09-23 00:00:00+00'),
    ('palavras-magicas', 'Palavras Mágicas', '🔤',
     'Um jogo de palavras divertido que amplia o vocabulário das crianças enquanto elas formam feitiços mágicos com letras.',
     'Vocabulário', (SELECT id FROM categories WHERE slug = '7-10'), '8+ anos', '2 a 4 jogadores', 74.90, 94.90, 12, FALSE, NULL),
    ('alvo-certeiro', 'Alvo Certeiro', '🎯',
     'Jogo de pontaria e agilidade que desenvolve a coordenação motora com muita diversão para toda a família.',
     'Coordenação motora', (SELECT id FROM categories WHERE slug = '4-6'), '4+ anos', '1 a 4 jogadores', 64.90, NULL, 25, FALSE, NULL),
    ('chocalho-das-cores', 'Chocalho das Cores', '🌈',
     'Jogo sensorial pensado para os pequenos, que estimula a percepção visual através de cores e sons.',
     'Percepção visual', (SELECT id FROM categories WHERE slug = 'ate-3'), '1+ ano', '1 a 2 jogadores', 54.90, NULL, 30, FALSE, TIMESTAMP WITH TIME ZONE '2026-09-18 00:00:00+00'),
    ('ilha-do-tesouro-cooperativa', 'Ilha do Tesouro Cooperativa', '🏝️',
     'Uma aventura cooperativa em que os jogadores se unem para encontrar o tesouro escondido antes que a ilha afunde.',
     'Trabalho em equipe', (SELECT id FROM categories WHERE slug = 'mais-10'), '10+ anos', '3 a 5 jogadores', 139.90, 169.90, 7, TRUE, NULL),
    ('empilha-bichos', 'Empilha Bichos', '🐘',
     'Jogo de empilhar bichinhos de madeira que desenvolve equilíbrio, paciência e coordenação motora fina.',
     'Coordenação motora', (SELECT id FROM categories WHERE slug = 'ate-3'), '2+ anos', '1 a 4 jogadores', 59.90, NULL, 14, FALSE, NULL),
    ('detetives-da-escola', 'Detetives da Escola', '🔍',
     'Um mistério para solucionar em equipe, exercitando o raciocínio lógico e a atenção aos detalhes.',
     'Raciocínio lógico', (SELECT id FROM categories WHERE slug = '7-10'), '7+ anos', '3 a 6 jogadores', 99.90, NULL, 11, FALSE, TIMESTAMP WITH TIME ZONE '2026-10-03 00:00:00+00'),
    ('torre-magica', 'Torre Mágica', '🗼',
     'Retire as peças sem derrubar a torre! Um clássico que desenvolve coordenação motora e paciência.',
     'Coordenação motora', (SELECT id FROM categories WHERE slug = '4-6'), '5+ anos', '2 a 4 jogadores', 79.90, 99.90, 0, FALSE, NULL),
    ('exploradores-do-espaco', 'Exploradores do Espaço', '🚀',
     'Uma aventura espacial que estimula a criatividade das crianças através de missões e histórias imaginativas.',
     'Criatividade', (SELECT id FROM categories WHERE slug = 'mais-10'), '10+ anos', '2 a 5 jogadores', 129.90, NULL, 16, FALSE, TIMESTAMP WITH TIME ZONE '2026-09-16 00:00:00+00');

INSERT INTO reviews (product_id, author_name, rating, comment, status) VALUES
    ((SELECT id FROM products WHERE slug = 'corrida-dos-sapos'), 'Fernanda M.', 5, 'Meu filho de 6 anos aprendeu a jogar rapidinho e não larga mais!', 'APPROVED'),
    ((SELECT id FROM products WHERE slug = 'corrida-dos-sapos'), 'Ricardo A.', 4, 'Ótima qualidade do material e as regras são bem simples.', 'APPROVED'),
    ((SELECT id FROM products WHERE slug = 'missao-no-castelo'), 'Juliana P.', 5, 'Jogamos em família e foi ótimo ver as crianças se ajudando em vez de competir.', 'APPROVED');
