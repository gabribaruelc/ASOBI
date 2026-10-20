"use client";

import { useState } from "react";
import Link from "next/link";
import styles from "./page.module.css";
import { apiFetch } from "../../lib/api";

const COPY = {
  BACK_IN_STOCK: {
    toggle: "🔔 Avise-me quando voltar",
    done: "Combinado! A gente manda um e-mail assim que este jogo voltar ao estoque.",
  },
  PROMOTION: {
    toggle: "🔔 Avise-me se entrar em promoção",
    done: "Combinado! A gente manda um e-mail se este jogo entrar em promoção.",
  },
  LOW_STOCK: {
    toggle: "⏳ Avise-me se estiver acabando",
    done: "Combinado! A gente manda um e-mail quando este jogo estiver nas últimas unidades.",
  },
};

// Só o e-mail do adulto, e só com a caixa de consentimento marcada (ela começa desmarcada).
// O aviso é enviado pelo notification-service; cada e-mail traz o link para cancelar.
export default function ProductAlertForm({ slug, type }) {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [consent, setConsent] = useState(false);
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const copy = COPY[type];

  async function handleSubmit(event) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      await apiFetch("/api/product-alerts", {
        method: "POST",
        body: JSON.stringify({ email, productSlug: slug, type, consent }),
      });
      setSent(true);
    } catch (err) {
      setError(
        err.fieldErrors?.email || err.fieldErrors?.consent || err.message,
      );
    } finally {
      setSending(false);
    }
  }

  if (sent) {
    return (
      <p className={styles.alertDone} role="status">
        {copy.done}
      </p>
    );
  }

  if (!open) {
    return (
      <button
        type="button"
        className={styles.alertToggle}
        onClick={() => setOpen(true)}
      >
        {copy.toggle}
      </button>
    );
  }

  return (
    <form className={styles.alertForm} onSubmit={handleSubmit}>
      <label className={styles.field}>
        <span>Seu e-mail</span>
        <input
          type="email"
          name="email"
          placeholder="voce@exemplo.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          maxLength={254}
          autoFocus
          required
        />
      </label>
      <label className={styles.alertConsent}>
        <input
          type="checkbox"
          name="consent"
          checked={consent}
          onChange={(e) => setConsent(e.target.checked)}
          required
        />
        <span>
          Quero receber este aviso por e-mail. Posso cancelar quando quiser
          pelo link que vem no próprio e-mail.
        </span>
      </label>
      {error && (
        <p className={styles.reviewNote} role="alert">
          {error}
        </p>
      )}
      <button type="submit" className={styles.reviewSubmit} disabled={sending}>
        {sending ? "Enviando…" : "Quero ser avisado"}
      </button>
      <p className={styles.reviewNote}>
        Usamos o seu e-mail só para este aviso. Veja a{" "}
        <Link href="/politica-de-privacidade">Política de Privacidade</Link>.
      </p>
    </form>
  );
}
