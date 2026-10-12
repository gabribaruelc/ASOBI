import styles from "./page.module.css";
import { getAbout } from "../lib/catalog";

// Texto reserva, usado só se o backend não responder. O conteúdo de verdade é o
// que a Priscila edita em /admin/sobre.
const FALLBACK_SOBRE = {
  heroText:
    'O nome ASOBI vem do japonês 遊び ("asobi"), que significa brincadeira. Acreditamos que é brincando que as crianças aprendem melhor — e é por isso que existimos: para levar até as famílias jogos de tabuleiro que unem diversão de verdade com desenvolvimento infantil.',
  values: [],
  missionEmoji: "🎲",
  missionTitle: "Ajudar cada família a escolher o jogo certo",
  missionText:
    "Em cada produto do nosso catálogo, mostramos a idade recomendada, o número de jogadores e a habilidade que aquele jogo estimula. Assim, escolher o presente certo fica mais simples e divertido.",
};

export default async function SobrePage() {
  const sobre = (await getAbout()) || FALLBACK_SOBRE;

  return (
    <main>
      <section className={styles.hero}>
        <div className="container">
          <p className={styles.kicker}>Nossa história</p>
          <h1>
            <span className={styles.logoAso}>aso</span>
            <span className={styles.logoBi}>bi</span> significa brincadeira
          </h1>
          <p className={styles.desc}>{sobre.heroText}</p>
        </div>
      </section>

      {sobre.values.length > 0 && (
        <section className={styles.section}>
          <div className="container">
            <div className={styles.sectionHead}>
              <p className={styles.kicker}>O que nos move</p>
              <h2>Nossos valores</h2>
            </div>

            <div className={styles.valuesGrid}>
              {sobre.values.map((value) => (
                <div key={value.title} className={styles.valueCard}>
                  <div className={styles.valueIcon} aria-hidden="true">
                    {value.icon}
                  </div>
                  <h3>{value.title}</h3>
                  <p>{value.text}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className={styles.section}>
        <div className="container">
          <div className={styles.mission}>
            <div className={styles.missionCard} aria-hidden="true">
              {sobre.missionEmoji}
            </div>
            <div>
              <p className={styles.kicker}>Nossa missão</p>
              <h2>{sobre.missionTitle}</h2>
              <p className={styles.desc}>{sobre.missionText}</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
