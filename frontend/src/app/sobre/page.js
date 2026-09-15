"use client";

import styles from "./page.module.css";
import { useSiteContent } from "../context/SiteContentContext";

export default function SobrePage() {
  const { sobre } = useSiteContent();

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
