"use client";

import { useState } from "react";
import Link from "next/link";
import styles from "./page.module.css";
import { apiFetch } from "./lib/api";

// Só o e-mail do adulto. A lista fica no painel (Newsletter), de onde o lojista exporta.
export default function NewsletterForm() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      await apiFetch("/api/newsletter-subscriptions", {
        method: "POST",
        body: JSON.stringify({ email }),
      });
      setSent(true);
    } catch (err) {
      setError(err.fieldErrors?.email || err.message);
    } finally {
      setSending(false);
    }
  }

  if (sent) {
    return (
      <p className={styles.newsletterDone} role="status">
        🎉 Pronto! Você vai receber as novidades da ASOBI.
      </p>
    );
  }

  return (
    <div className={styles.newsletterBox}>
      <form className={styles.newsletterForm} onSubmit={handleSubmit}>
        <input
          type="email"
          name="email"
          placeholder="Seu melhor e-mail"
          aria-label="Seu e-mail"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          maxLength={254}
          required
        />
        <button type="submit" className={styles.btnPrimary} disabled={sending}>
          {sending ? "Enviando…" : "Quero receber"}
        </button>
      </form>
      {error && (
        <p className={styles.newsletterNote} role="alert">
          {error}
        </p>
      )}
      <p className={styles.newsletterNote}>
        Ao se inscrever, você aceita receber e-mails da ASOBI. Veja a{" "}
        <Link href="/politica-de-privacidade">Política de Privacidade</Link>.
      </p>
    </div>
  );
}
