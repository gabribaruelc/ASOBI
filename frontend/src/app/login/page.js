import Link from "next/link";
import styles from "./page.module.css";
import GoogleButton from "../components/GoogleButton";

export const metadata = {
  title: "Entrar | ASOBI",
  description: "Acesse sua conta ASOBI para acompanhar seus pedidos.",
};

export default function LoginPage() {
  return (
    <main className={styles.main}>
      <div className={styles.card}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoAso}>aso</span>
          <span className={styles.logoBi}>bi</span>
        </Link>

        <h1>Entrar na sua conta</h1>
        <p className={styles.subtitle}>
          Acompanhe seus pedidos e finalize suas compras mais rápido. É a
          primeira vez? Sua conta é criada na hora, com o seu Google.
        </p>

        <GoogleButton label="Continuar com Google" />

        <p className={styles.switch}>
          Prefere não criar conta? <Link href="/jogos">Compre sem cadastro</Link>
        </p>
      </div>
    </main>
  );
}
