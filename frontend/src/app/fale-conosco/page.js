import styles from "../components/PolicyPage.module.css";
import { getSiteInfo } from "../lib/catalog";

export const metadata = {
  title: "Fale Conosco | ASOBI",
  description: "Canais de atendimento da ASOBI.",
};

// Os canais são os que a Priscila preencheu em "Contato e rodapé" no painel.
export default async function FaleConoscoPage() {
  const siteInfo = await getSiteInfo();
  const hasChannel = siteInfo.contactEmail || siteInfo.contactPhone;

  return (
    <main className={styles.main}>
      <div className={`container ${styles.content}`}>
        <h1>Fale Conosco</h1>
        <p>
          Dúvida sobre um jogo, um pedido ou uma troca? Escolha o canal que preferir —
          a gente responde o mais rápido possível.
        </p>

        {hasChannel ? (
          <div className={styles.contactList}>
            {siteInfo.contactPhone && (
              <div className={styles.contactCard}>
                <span className={styles.contactIcon} aria-hidden="true">💬</span>
                <span className={styles.contactLabel}>WhatsApp</span>
                {siteInfo.whatsappUrl ? (
                  <a href={siteInfo.whatsappUrl} target="_blank" rel="noopener noreferrer">
                    {siteInfo.contactPhone}
                  </a>
                ) : (
                  <span>{siteInfo.contactPhone}</span>
                )}
              </div>
            )}
            {siteInfo.contactEmail && (
              <div className={styles.contactCard}>
                <span className={styles.contactIcon} aria-hidden="true">✉️</span>
                <span className={styles.contactLabel}>E-mail</span>
                <a href={`mailto:${siteInfo.contactEmail}`}>{siteInfo.contactEmail}</a>
              </div>
            )}
            {siteInfo.businessHours && (
              <div className={styles.contactCard}>
                <span className={styles.contactIcon} aria-hidden="true">🕒</span>
                <span className={styles.contactLabel}>Horário de atendimento</span>
                <span>{siteInfo.businessHours}</span>
              </div>
            )}
          </div>
        ) : (
          <p>Não conseguimos carregar os canais de atendimento agora. Tente de novo em instantes.</p>
        )}

        {siteInfo.address && <p>{siteInfo.address}</p>}
      </div>
    </main>
  );
}
