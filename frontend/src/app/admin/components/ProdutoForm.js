"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import styles from "../admin.module.css";
import { AGE_FILTERS } from "../../data/products";

const EMPTY_FORM = {
  icon: "🎲",
  name: "",
  skill: "",
  ageKey: AGE_FILTERS[0].key,
  age: "",
  players: "",
  price: "",
  stock: 0,
  estilo: "",
  newDays: "",
  isPromo: false,
  originalPrice: "",
  description: "",
};

function slugify(name) {
  return name
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

function toFormState(product) {
  if (!product) return EMPTY_FORM;
  return {
    icon: product.icon,
    name: product.name,
    skill: product.skill,
    ageKey: product.ageKey,
    age: product.age,
    players: product.players,
    price: product.price,
    stock: product.stock,
    estilo: product.estilo || "",
    newDays: product.newUntil
      ? Math.max(
          0,
          Math.ceil((new Date(product.newUntil).getTime() - Date.now()) / 86400000)
        )
      : "",
    isPromo: Boolean(product.promo),
    originalPrice: product.promo?.originalPrice ?? "",
    description: product.description,
  };
}

export default function ProdutoForm({ initialProduct, onSubmit, submitLabel }) {
  const router = useRouter();
  const [form, setForm] = useState(() => toFormState(initialProduct));

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();

    const newUntil = form.newDays
      ? new Date(Date.now() + Number(form.newDays) * 86400000).toISOString()
      : null;

    const product = {
      slug: initialProduct ? initialProduct.slug : slugify(form.name),
      icon: form.icon || "🎲",
      name: form.name,
      skill: form.skill,
      ageKey: form.ageKey,
      age: form.age,
      players: form.players,
      price: Number(form.price),
      stock: Math.max(0, Number(form.stock) || 0),
      estilo: form.estilo || null,
      promo:
        form.isPromo && form.originalPrice
          ? { originalPrice: Number(form.originalPrice) }
          : null,
      newUntil,
      description: form.description,
      reviews: initialProduct?.reviews || [],
    };

    onSubmit(product);
    router.push("/admin/produtos");
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formGrid}>
        <label className={styles.field}>
          <span>Ícone (emoji)</span>
          <input
            value={form.icon}
            onChange={(e) => handleChange("icon", e.target.value)}
          />
        </label>

        <label className={styles.field}>
          <span>Nome do jogo</span>
          <input
            value={form.name}
            onChange={(e) => handleChange("name", e.target.value)}
            required
          />
        </label>

        <label className={styles.field}>
          <span>Habilidade estimulada</span>
          <input
            value={form.skill}
            onChange={(e) => handleChange("skill", e.target.value)}
            required
          />
        </label>

        <label className={styles.field}>
          <span>Faixa etária (filtro)</span>
          <select
            value={form.ageKey}
            onChange={(e) => handleChange("ageKey", e.target.value)}
          >
            {AGE_FILTERS.map((filter) => (
              <option key={filter.key} value={filter.key}>
                {filter.label}
              </option>
            ))}
          </select>
        </label>

        <label className={styles.field}>
          <span>Idade recomendada (texto)</span>
          <input
            value={form.age}
            onChange={(e) => handleChange("age", e.target.value)}
            placeholder="ex: 6+ anos"
            required
          />
        </label>

        <label className={styles.field}>
          <span>Jogadores</span>
          <input
            value={form.players}
            onChange={(e) => handleChange("players", e.target.value)}
            placeholder="ex: 2 a 4 jogadores"
            required
          />
        </label>

        <label className={styles.field}>
          <span>Preço (R$)</span>
          <input
            type="number"
            step="0.01"
            min="0"
            value={form.price}
            onChange={(e) => handleChange("price", e.target.value)}
            required
          />
        </label>

        <label className={styles.field}>
          <span>Estoque</span>
          <input
            type="number"
            min="0"
            value={form.stock}
            onChange={(e) => handleChange("stock", e.target.value)}
            required
          />
        </label>

        <label className={styles.checkboxField}>
          <input
            type="checkbox"
            checked={form.estilo === "cooperativos"}
            onChange={(e) =>
              handleChange("estilo", e.target.checked ? "cooperativos" : "")
            }
          />
          <span>Jogo cooperativo</span>
        </label>

        <label className={styles.field}>
          <span>Dias em &quot;Novidades&quot; (vazio = não aparece)</span>
          <input
            type="number"
            min="0"
            value={form.newDays}
            onChange={(e) => handleChange("newDays", e.target.value)}
            placeholder="ex: 14"
          />
        </label>

        <label className={styles.checkboxField}>
          <input
            type="checkbox"
            checked={form.isPromo}
            onChange={(e) => handleChange("isPromo", e.target.checked)}
          />
          <span>Em promoção</span>
        </label>

        {form.isPromo && (
          <label className={styles.field}>
            <span>Preço original (de)</span>
            <input
              type="number"
              step="0.01"
              min="0"
              value={form.originalPrice}
              onChange={(e) => handleChange("originalPrice", e.target.value)}
              required
            />
          </label>
        )}
      </div>

      <label className={styles.field}>
        <span>Descrição</span>
        <textarea
          rows={4}
          value={form.description}
          onChange={(e) => handleChange("description", e.target.value)}
          required
        />
      </label>

      <button type="submit" className={styles.primaryButton}>
        {submitLabel}
      </button>
    </form>
  );
}
