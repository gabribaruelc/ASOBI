"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import styles from "./page.module.css";
import { useAdmin } from "../../context/AdminContext";

export default function AdminLoginPage() {
  const { login } = useAdmin();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [error, setError] = useState(null);

  function handleSubmit(event) {
    event.preventDefault();
    const ok = login(email);
    if (ok) {
      router.push("/admin");
    } else {
      setError("Esse e-mail não tem acesso de admin.");
    }
  }

  return (
    <main className={styles.main}>
      <div className={styles.card}>
        <Link href="/" className={styles.logo}>
          <span className={styles.logoAso}>aso</span>
          <span className={styles.logoBi}>bi</span>
        </Link>

        <h1>Painel admin</h1>
        <p className={styles.subtitle}>
          Acesso restrito a e-mails autorizados pela loja.
        </p>

        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.field}>
            <span>E-mail de admin</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="voce@asobi.com.br"
              required
            />
          </label>

          {error && <p className={styles.error}>{error}</p>}

          <button type="submit" className={styles.submit}>
            Entrar no painel
          </button>
        </form>

        <p className={styles.note}>
          Primeiro acesso: use <strong>priscila@asobi.com.br</strong> e depois
          cadastre o e-mail real da Priscila em Admins. Esse login por e-mail é
          provisório (localStorage) — quando o Supabase Auth entrar, ele
          continua checando a mesma lista de admins, só que no backend.
        </p>
      </div>
    </main>
  );
}
