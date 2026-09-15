import Link from "next/link";
import styles from "./page.module.css";
import GoogleButton from "../components/GoogleButton";

export const metadata = {
  title: "Cadastrar | ASOBI",
  description: "Crie sua conta ASOBI para comprar jogos de tabuleiro infantis.",
};

export default function CadastroPage() {
  return (
    <main className={styles.main}>
      <div className={styles.card}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoAso}>aso</span>
          <span className={styles.logoBi}>bi</span>
        </Link>

        <h1>Criar sua conta</h1>
        <p className={styles.subtitle}>
          Cadastre-se para acompanhar pedidos e agilizar suas próximas
          compras.
        </p>

        <GoogleButton label="Cadastrar com Google" />

        <div className={styles.divider}>
          <span>ou cadastre-se com e-mail</span>
        </div>

        <form className={styles.form}>
          <label className={styles.field}>
            <span>Nome completo</span>
            <input type="text" name="name" placeholder="Seu nome" required />
          </label>
          <label className={styles.field}>
            <span>E-mail</span>
            <input type="email" name="email" placeholder="voce@email.com" required />
          </label>
          <label className={styles.field}>
            <span>Senha</span>
            <input type="password" name="password" placeholder="••••••••" required />
          </label>
          <label className={styles.field}>
            <span>Confirmar senha</span>
            <input
              type="password"
              name="confirmPassword"
              placeholder="••••••••"
              required
            />
          </label>

          <label className={styles.checkboxField}>
            <input type="checkbox" name="terms" required />
            <span>
              Li e aceito os{" "}
              <Link href="/termos-de-uso">Termos de Uso</Link> e a{" "}
              <Link href="/politica-de-privacidade">
                Política de Privacidade
              </Link>
              .
            </span>
          </label>

          <button type="submit" className={styles.submit}>
            Cadastrar
          </button>
        </form>

        <p className={styles.lgpdNote}>
          O cadastro deve ser feito pelo responsável adulto — não coletamos
          dados da criança além da faixa etária e preferências de jogo.
        </p>

        <p className={styles.switch}>
          Já tem conta? <Link href="/login">Entrar</Link>
        </p>
      </div>
    </main>
  );
}
