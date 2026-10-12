"use client";

import { useState } from "react";
import styles from "./page.module.css";
import { apiFetch } from "../../lib/api";

// A avaliação vai para o backend como pendente e só aparece na ficha depois de
// aprovada no painel.
export default function ReviewForm({ slug }) {
  const [name, setName] = useState("");
  const [rating, setRating] = useState("5");
  const [comment, setComment] = useState("");
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      await apiFetch(`/api/products/${encodeURIComponent(slug)}/reviews`, {
        method: "POST",
        body: JSON.stringify({ name, rating: Number(rating), comment }),
      });
      setSent(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  }

  if (sent) {
    return (
      <p className={styles.reviewNote}>
        Recebemos sua avaliação! Ela aparece aqui depois que a nossa equipe
        aprovar.
      </p>
    );
  }

  return (
    <form className={styles.reviewForm} onSubmit={handleSubmit}>
      <h3>Deixe sua avaliação</h3>
      <div className={styles.reviewFormRow}>
        <label className={styles.field}>
          <span>Seu nome</span>
          <input
            type="text"
            name="name"
            placeholder="Seu nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={80}
            required
          />
        </label>
        <label className={styles.field}>
          <span>Nota</span>
          <select
            name="rating"
            value={rating}
            onChange={(e) => setRating(e.target.value)}
          >
            <option value="5">★★★★★</option>
            <option value="4">★★★★☆</option>
            <option value="3">★★★☆☆</option>
            <option value="2">★★☆☆☆</option>
            <option value="1">★☆☆☆☆</option>
          </select>
        </label>
      </div>
      <label className={styles.field}>
        <span>Comentário</span>
        <textarea
          name="comment"
          rows={3}
          placeholder="Conte como foi a experiência com esse jogo"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          maxLength={1000}
          required
        />
      </label>
      {error && (
        <p className={styles.reviewNote} role="alert">
          {error}
        </p>
      )}
      <button type="submit" className={styles.reviewSubmit} disabled={sending}>
        {sending ? "Enviando…" : "Enviar avaliação"}
      </button>
      <p className={styles.reviewNote}>
        Sua avaliação passa por aprovação da nossa equipe antes de aparecer na
        página.
      </p>
    </form>
  );
}
