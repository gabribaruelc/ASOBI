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
          Acompanhe seus pedidos e finalize suas compras mais rápido.
        </p>

        <GoogleButton label="Entrar com Google" />

        <div className={styles.divider}>
          <span>ou entre com e-mail</span>
        </div>

        <form className={styles.form}>
          <label className={styles.field}>
            <span>E-mail</span>
            <input type="email" name="email" placeholder="voce@email.com" required />
          </label>
          <label className={styles.field}>
            <span>Senha</span>
            <input type="password" name="password" placeholder="••••••••" required />
          </label>
          <Link href="/esqueci-minha-senha" className={styles.forgot}>
            Esqueci minha senha
          </Link>
          <button type="submit" className={styles.submit}>
            Entrar
          </button>
        </form>

        <p className={styles.switch}>
          Ainda não tem conta? <Link href="/cadastro">Cadastre-se</Link>
        </p>
      </div>
    </main>
  );
}
