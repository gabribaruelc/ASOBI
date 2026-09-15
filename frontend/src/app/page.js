import Link from "next/link";
import styles from "./page.module.css";

const SKILLS = [
  {
    icon: "🧠",
    bg: "#eaf2ff",
    title: "Raciocínio lógico",
    text: "Jogos de estratégia e quebra-cabeças que desafiam a mente.",
  },
  {
    icon: "🤝",
    bg: "#ffeaf3",
    title: "Trabalho em equipe",
    text: "Brincadeiras cooperativas que ensinam a jogar junto.",
  },
  {
    icon: "✋",
    bg: "#eaf2ff",
    title: "Coordenação motora",
    text: "Peças e movimentos que ajudam no desenvolvimento motor.",
  },
  {
    icon: "🎨",
    bg: "#ffeaf3",
    title: "Criatividade",
    text: "Histórias e desafios que estimulam a imaginação.",
  },
];

const CATEGORIES = [
  { icon: "🍼", label: "Até 3 anos", color: "#2e7cf6" },
  { icon: "🧸", label: "4 a 6 anos", color: "#ff5fa2" },
  { icon: "🧩", label: "7 a 10 anos", color: "#1e5fd1" },
  { icon: "🚀", label: "+10 anos", color: "#e8408a" },
  { icon: "🤝", label: "Cooperativos", color: "#2e7cf6" },
];

const PRODUCTS = [
  {
    icon: "🐸",
    skill: "Raciocínio lógico",
    name: "Corrida dos Sapos",
    age: "6+ anos · 2 a 4 jogadores",
    price: "R$ 89,90",
  },
  {
    icon: "🏰",
    skill: "Trabalho em equipe",
    name: "Missão no Castelo",
    age: "7+ anos · 2 a 6 jogadores",
    price: "R$ 119,90",
  },
  {
    icon: "🔤",
    skill: "Vocabulário",
    name: "Palavras Mágicas",
    age: "8+ anos · 2 a 4 jogadores",
    price: "R$ 74,90",
  },
  {
    icon: "🎯",
    skill: "Coordenação motora",
    name: "Alvo Certeiro",
    age: "4+ anos · 1 a 4 jogadores",
    price: "R$ 64,90",
  },
];

export default function Home() {
  return (
    <main>
      <section className={styles.hero}>
        <div className={`container ${styles.heroInner}`}>
          <div className={styles.heroText}>
            <span className={styles.eyebrow}>🌟 Aprender brincando</span>
            <h1>
              Jogos de tabuleiro que ensinam <span>enquanto divertem</span>
            </h1>
            <p>
              Uma curadoria de jogos pensados para o desenvolvimento infantil
              — cada um indica a habilidade que estimula, para ajudar você a
              escolher o presente certo.
            </p>
            <div className={styles.heroActions}>
              <Link href="/catalogo" className={styles.btnPrimary}>
                Ver catálogo
              </Link>
              <Link href="/sobre" className={styles.btnSecondary}>
                Conhecer a loja
              </Link>
            </div>
          </div>

          <div className={styles.heroVisual}>
            <span className={`${styles.heroBadge} ${styles.heroBadgeTop}`}>
              🧠 Estimula raciocínio
            </span>
            <div className={styles.heroCard} aria-hidden="true">
              🎲
            </div>
            <span className={`${styles.heroBadge} ${styles.heroBadgeBottom}`}>
              🤝 Diversão em família
            </span>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          <div className={styles.sectionHead}>
            <p className={styles.kicker}>Escolha com confiança</p>
            <h2>Cada jogo mostra a habilidade que estimula</h2>
            <p className={styles.desc}>
              Assim você sabe exatamente o que seu filho vai desenvolver
              brincando — do raciocínio lógico ao trabalho em equipe.
            </p>
          </div>

          <div className={styles.skillsGrid}>
            {SKILLS.map((skill) => (
              <div key={skill.title} className={styles.skillCard}>
                <div
                  className={styles.skillIcon}
                  style={{ background: skill.bg }}
                  aria-hidden="true"
                >
                  {skill.icon}
                </div>
                <h3>{skill.title}</h3>
                <p>{skill.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          <div className={styles.sectionHead}>
            <p className={styles.kicker}>Categorias</p>
            <h2>Encontre pela faixa etária</h2>
          </div>

          <div className={styles.categoriesGrid}>
            {CATEGORIES.map((category) => (
              <Link
                key={category.label}
                href="/categorias"
                className={styles.categoryCard}
                style={{ background: category.color }}
              >
                <span className={styles.emoji} aria-hidden="true">
                  {category.icon}
                </span>
                <span className={styles.age}>{category.label}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          <div className={styles.sectionHead}>
            <p className={styles.kicker}>Destaques</p>
            <h2>Mais queridinhos da turma</h2>
          </div>

          <div className={styles.productsGrid}>
            {PRODUCTS.map((product) => (
              <div key={product.name} className={styles.productCard}>
                <div className={styles.productImage} aria-hidden="true">
                  {product.icon}
                </div>
                <div className={styles.productBody}>
                  <span className={styles.productSkill}>{product.skill}</span>
                  <h3>{product.name}</h3>
                  <p className={styles.productMeta}>{product.age}</p>
                  <div className={styles.productFooter}>
                    <span className={styles.productPrice}>{product.price}</span>
                    <button className={styles.productButton}>Ver mais</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <div className="container">
          <div className={styles.newsletter}>
            <div>
              <h2>Novidades e dicas de brincadeiras</h2>
              <p>
                Receba avisos de novos jogos e sugestões de atividades para
                brincar com seus filhos.
              </p>
            </div>
            <form className={styles.newsletterForm}>
              <input type="email" placeholder="Seu melhor e-mail" />
              <button type="submit" className={styles.btnPrimary}>
                Quero receber
              </button>
            </form>
          </div>
        </div>
      </section>
    </main>
  );
}
