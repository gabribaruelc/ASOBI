"use client";

import { useState } from "react";
import styles from "../admin.module.css";
import { useSiteContent } from "../../context/SiteContentContext";

export default function AdminSobrePage() {
  const { sobre, updateSobre } = useSiteContent();
  const [form, setForm] = useState(sobre);
  const [saved, setSaved] = useState(false);

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setSaved(false);
  }

  function handleValueChange(index, field, value) {
    const values = form.values.map((item, i) =>
      i === index ? { ...item, [field]: value } : item
    );
    handleChange("values", values);
  }

  function handleSubmit(event) {
    event.preventDefault();
    updateSobre(form);
    setSaved(true);
  }

  return (
    <div>
      <h1 className={styles.pageTitle}>Página Sobre</h1>
      <p className={styles.pageDesc} style={{ marginBottom: "24px" }}>
        Esse texto e esses ícones aparecem em /sobre.
      </p>

      <form className={styles.form} onSubmit={handleSubmit}>
        <label className={styles.field}>
          <span>Texto principal da história</span>
          <textarea
            rows={4}
            value={form.heroText}
            onChange={(e) => handleChange("heroText", e.target.value)}
          />
        </label>

        <h2 className={styles.sectionTitle}>Valores</h2>
        <div className={styles.formGrid}>
          {form.values.map((value, index) => (
            <div key={index} className={styles.valueEditCard}>
              <label className={styles.field}>
                <span>Ícone</span>
                <input
                  value={value.icon}
                  onChange={(e) => handleValueChange(index, "icon", e.target.value)}
                />
              </label>
              <label className={styles.field}>
                <span>Título</span>
                <input
                  value={value.title}
                  onChange={(e) => handleValueChange(index, "title", e.target.value)}
                />
              </label>
              <label className={styles.field}>
                <span>Texto</span>
                <textarea
                  rows={2}
                  value={value.text}
                  onChange={(e) => handleValueChange(index, "text", e.target.value)}
                />
              </label>
            </div>
          ))}
        </div>

        <h2 className={styles.sectionTitle}>Missão</h2>
        <div className={styles.formGrid}>
          <label className={styles.field}>
            <span>Ícone da missão</span>
            <input
              value={form.missionEmoji}
              onChange={(e) => handleChange("missionEmoji", e.target.value)}
            />
          </label>
          <label className={styles.field}>
            <span>Título da missão</span>
            <input
              value={form.missionTitle}
              onChange={(e) => handleChange("missionTitle", e.target.value)}
            />
          </label>
        </div>
        <label className={styles.field}>
          <span>Texto da missão</span>
          <textarea
            rows={4}
            value={form.missionText}
            onChange={(e) => handleChange("missionText", e.target.value)}
          />
        </label>

        <button type="submit" className={styles.primaryButton}>
          Salvar alterações
        </button>
        {saved && <p className={styles.savedNote}>Salvo! Confira em /sobre.</p>}
      </form>
    </div>
  );
}
