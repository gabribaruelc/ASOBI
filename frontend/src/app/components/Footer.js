import Link from "next/link";
import styles from "./Footer.module.css";

export default function Footer() {
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
          <div className={styles.social}>
            <a href="#" aria-label="Instagram">
              📸
            </a>
            <a href="#" aria-label="Facebook">
              📘
            </a>
            <a href="#" aria-label="WhatsApp">
              💬
            </a>
          </div>
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
            <p className={styles.contactItem}>
              <span aria-hidden="true">✉️</span> contato@asobi.com.br
            </p>
            <p className={styles.contactItem}>
              <span aria-hidden="true">💬</span> (00) 00000-0000
            </p>
            <p className={styles.contactItem}>
              <span aria-hidden="true">🕒</span> Seg. a sex., 9h às 18h
            </p>
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
        <span>Razão social e CNPJ a definir</span>
      </div>
    </footer>
  );
}
