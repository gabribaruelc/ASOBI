import Link from "next/link";
import styles from "./Footer.module.css";

const SOCIAL_LINKS = [
  { field: "instagramUrl", label: "Instagram", icon: "📸" },
  { field: "facebookUrl", label: "Facebook", icon: "📘" },
  { field: "tiktokUrl", label: "TikTok", icon: "🎵" },
  { field: "whatsappUrl", label: "WhatsApp", icon: "💬" },
];

// siteInfo vem do servidor (layout), com o que a Priscila preencheu em "Contato e rodapé"
// no painel. O que está vazio não aparece.
export default function Footer({ siteInfo }) {
  const social = SOCIAL_LINKS.filter((link) => siteInfo[link.field]);
  const legalLine = [siteInfo.legalName, siteInfo.cnpj && `CNPJ ${siteInfo.cnpj}`]
    .filter(Boolean)
    .join(" · ");

  return (
    <footer className={styles.footer}>
      <div className={`container ${styles.top}`}>
        <div className={styles.brand}>
          <div className={styles.brandName}>
            <span className={styles.brandNameAso}>aso</span>
            <span className={styles.brandNameBi}>bi</span>
          </div>
          <p className={styles.brandText}>
            Jogos de tabuleiro selecionados para estimular o aprendizado das
            crianças através da brincadeira: raciocínio lógico, trabalho em
            equipe, coordenação motora e muito mais.
          </p>
          {social.length > 0 && (
            <div className={styles.social}>
              {social.map((link) => (
                <a
                  key={link.field}
                  href={siteInfo[link.field]}
                  aria-label={link.label}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {link.icon}
                </a>
              ))}
            </div>
          )}
        </div>

        <div>
          <p className={styles.colTitle}>Institucional</p>
          <div className={styles.links}>
            <Link href="/sobre">Sobre a ASOBI</Link>
            <Link href="/politica-de-privacidade">Política de Privacidade</Link>
            <Link href="/trocas-devolucoes-garantia">Trocas, Devoluções e Garantia</Link>
            <Link href="/politica-de-frete">Política de Frete</Link>
            <Link href="/fale-conosco">Fale Conosco</Link>
          </div>
        </div>

        <div>
          <p className={styles.colTitle}>Atendimento</p>
          <div className={styles.links}>
            {siteInfo.contactEmail && (
              <a className={styles.contactItem} href={`mailto:${siteInfo.contactEmail}`}>
                <span aria-hidden="true">✉️</span> {siteInfo.contactEmail}
              </a>
            )}
            {siteInfo.contactPhone && (
              <a
                className={styles.contactItem}
                href={siteInfo.whatsappUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <span aria-hidden="true">💬</span> {siteInfo.contactPhone}
              </a>
            )}
            {siteInfo.businessHours && (
              <p className={styles.contactItem}>
                <span aria-hidden="true">🕒</span> {siteInfo.businessHours}
              </p>
            )}
          </div>
        </div>

        <div>
          <p className={styles.colTitle}>Formas de pagamento</p>
          <div className={styles.payments}>
            <span className={styles.paymentBadge}>Pix</span>
            <span className={styles.paymentBadge}>Cartão de crédito</span>
            <span className={styles.paymentBadge}>Boleto</span>
          </div>
          <p className={styles.contactItem} style={{ marginTop: "14px" }}>
            Pagamentos processados via Mercado Pago
          </p>
        </div>
      </div>

      <div className={`container ${styles.bottom}`}>
        <span>© {new Date().getFullYear()} ASOBI. Todos os direitos reservados.</span>
        {legalLine && <span>{legalLine}</span>}
      </div>
    </footer>
  );
}
