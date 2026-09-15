"use client";

import { useState } from "react";
import styles from "./AddToCartBox.module.css";
import { useCart } from "../context/CartContext";

export default function AddToCartBox({ slug, outOfStock }) {
  const { addItem } = useCart();
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);

  function handleAdd() {
    addItem(slug, quantity);
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  }

  if (outOfStock) {
    return (
      <div className={styles.buyRow}>
        <p className={styles.outOfStock}>
          Produto esgotado no momento. Volte em breve!
        </p>
      </div>
    );
  }

  return (
    <div className={styles.buyRow}>
      <div className={styles.quantity}>
        <button
          type="button"
          onClick={() => setQuantity((q) => Math.max(1, q - 1))}
          aria-label="Diminuir quantidade"
        >
          −
        </button>
        <span>{quantity}</span>
        <button
          type="button"
          onClick={() => setQuantity((q) => q + 1)}
          aria-label="Aumentar quantidade"
        >
          +
        </button>
      </div>
      <button type="button" className={styles.addToCart} onClick={handleAdd}>
        {added ? "Adicionado! ✓" : "Adicionar ao carrinho"}
      </button>
    </div>
  );
}
